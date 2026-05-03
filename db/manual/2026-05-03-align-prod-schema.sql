-- One-off production schema alignment for the legacy flip_sync database.
-- The running application uses ddl-auto=none in prod, so this script keeps
-- legacy data while bringing the schema in line with the current entities.

START TRANSACTION;

ALTER TABLE `users`
    MODIFY COLUMN `username` VARCHAR(255) NOT NULL;

ALTER TABLE `email_verify`
    MODIFY COLUMN `email` VARCHAR(255) NOT NULL,
    MODIFY COLUMN `try_count` INT NOT NULL DEFAULT 0;

ALTER TABLE `group`
    DROP INDEX `group_pk`,
    ADD COLUMN `organization_id` BIGINT NULL AFTER `creator_id`,
    ADD COLUMN `max_member_count` INT NULL AFTER `organization_id`,
    ADD COLUMN `room_password` VARCHAR(255) NULL AFTER `max_member_count`;

CREATE TABLE `organizations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `invite_code` VARCHAR(20) NOT NULL,
    `creator_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_organizations` PRIMARY KEY (`id`),
    CONSTRAINT `uk_organizations_invite_code` UNIQUE (`invite_code`),
    KEY `organizations_creator_id_index` (`creator_id`),
    CONSTRAINT `fk_organizations_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`)
);

INSERT INTO `organizations` (`name`, `invite_code`, `creator_id`, `created_at`, `modified_at`)
SELECT
    CONCAT('Legacy Organization ', legacy_creator.`creator_id`) AS `name`,
    CONCAT('LEGACY', LPAD(legacy_creator.`creator_id`, 8, '0')) AS `invite_code`,
    legacy_creator.`creator_id`,
    NOW(),
    NOW()
FROM (
    SELECT DISTINCT `creator_id`
    FROM `group`
) legacy_creator;

UPDATE `group` g
JOIN `organizations` o
    ON o.`invite_code` = CONCAT('LEGACY', LPAD(g.`creator_id`, 8, '0'))
SET
    g.`organization_id` = o.`id`,
    g.`max_member_count` = COALESCE(g.`max_member_count`, 10)
WHERE g.`organization_id` IS NULL
   OR g.`max_member_count` IS NULL;

CREATE TABLE `organization_members` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `organization_id` BIGINT NOT NULL,
    `users_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_organization_members` PRIMARY KEY (`id`),
    CONSTRAINT `uk_organization_members_organization_user` UNIQUE (`organization_id`, `users_id`),
    KEY `organization_members_organization_id_index` (`organization_id`),
    KEY `organization_members_users_id_index` (`users_id`),
    CONSTRAINT `fk_organization_members_organization_id`
        FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_organization_members_users_id`
        FOREIGN KEY (`users_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

INSERT INTO `organization_members` (`organization_id`, `users_id`, `role`, `created_at`, `modified_at`)
SELECT
    o.`id`,
    o.`creator_id`,
    'LEADER',
    NOW(),
    NOW()
FROM `organizations` o;

INSERT INTO `organization_members` (`organization_id`, `users_id`, `role`, `created_at`, `modified_at`)
SELECT DISTINCT
    g.`organization_id`,
    gu.`users_id`,
    'MEMBER',
    NOW(),
    NOW()
FROM `group_users` gu
JOIN `group` g
    ON g.`id` = gu.`group_id`
LEFT JOIN `organization_members` om
    ON om.`organization_id` = g.`organization_id`
   AND om.`users_id` = gu.`users_id`
WHERE g.`organization_id` IS NOT NULL
  AND om.`id` IS NULL;

CREATE TABLE `organization_score` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `organization_id` BIGINT NOT NULL,
    `title` VARCHAR(50) NOT NULL,
    `code` VARCHAR(30) NOT NULL,
    `singer` VARCHAR(50) NOT NULL,
    `uploaded_user_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_organization_score` PRIMARY KEY (`id`),
    KEY `organization_score_organization_id_index` (`organization_id`),
    KEY `organization_score_title_index` (`title`),
    KEY `organization_score_code_index` (`code`),
    KEY `organization_score_singer_index` (`singer`),
    CONSTRAINT `fk_organization_score_organization_id`
        FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`) ON DELETE CASCADE
);

CREATE TABLE `organization_score_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `organization_score_id` BIGINT NOT NULL,
    `order` INT NOT NULL,
    `url` VARCHAR(255) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `pk_organization_score_image` PRIMARY KEY (`id`),
    KEY `organization_score_image_score_id_index` (`organization_score_id`),
    CONSTRAINT `fk_organization_score_image_score_id`
        FOREIGN KEY (`organization_score_id`) REFERENCES `organization_score` (`id`) ON DELETE CASCADE
);

ALTER TABLE `email_verify`
    ADD CONSTRAINT `uk_email_verify_email` UNIQUE (`email`);

DROP INDEX `email_verify_email_index` ON `email_verify`;

ALTER TABLE `group`
    MODIFY COLUMN `organization_id` BIGINT NOT NULL,
    MODIFY COLUMN `max_member_count` INT NOT NULL,
    ADD KEY `group_organization_id_index` (`organization_id`),
    ADD CONSTRAINT `fk_group_organization_id`
        FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`);

ALTER TABLE `group_users`
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `score`
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `score_image`
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN `modified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

DROP INDEX `users_username_index` ON `users`;

COMMIT;
