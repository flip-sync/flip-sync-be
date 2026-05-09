CREATE TABLE IF NOT EXISTS flip_sync.app_version_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    platform VARCHAR(20) NOT NULL,
    latest_version VARCHAR(30) NOT NULL,
    latest_build_version INT NOT NULL,
    minimum_build_version INT NOT NULL,
    store_url VARCHAR(500) NOT NULL,
    force_update_message VARCHAR(500) NOT NULL,
    optional_update_message VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_version_policies_platform (platform)
);

INSERT INTO flip_sync.app_version_policies (
    platform,
    latest_version,
    latest_build_version,
    minimum_build_version,
    store_url,
    force_update_message,
    optional_update_message
) VALUES (
    'android',
    '1.0.0',
    11,
    1,
    'https://play.google.com/store/apps/details?id=com.fliplyze.flipsync',
    '새 버전으로 업데이트가 필요합니다.',
    '새 버전이 준비되었습니다.'
) ON DUPLICATE KEY UPDATE
    latest_version = VALUES(latest_version),
    latest_build_version = VALUES(latest_build_version),
    minimum_build_version = VALUES(minimum_build_version),
    store_url = VALUES(store_url),
    force_update_message = VALUES(force_update_message),
    optional_update_message = VALUES(optional_update_message);

-- After uploading a new AAB to Play internal testing, update latest_build_version.
-- Raise minimum_build_version only when older installed apps must be blocked.
UPDATE flip_sync.app_version_policies
SET latest_version = '1.0.0',
    latest_build_version = 11,
    minimum_build_version = 1
WHERE platform = 'android';
