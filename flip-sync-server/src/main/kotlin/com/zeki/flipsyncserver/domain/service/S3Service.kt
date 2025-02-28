package com.zeki.flipsyncserver.domain.service

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.IOException
import java.util.*
import java.util.function.Consumer

@Service
class S3Service(
    private val amazonS3Client: AmazonS3Client,

    @Value("\${cloud.aws.s3.bucket}")
    private val bucket: String,
    @Value("\${cloud.aws.s3.object-link}")
    private val objectLink: String,
    @Value("\${cloud.aws.s3.link-start}")
    private val linkStart: String
) {
    companion object {
        const val DIR = ""
    }

    fun createUrl(file: MultipartFile, path: String): String {
        return putFile(file, path)
    }

    private fun putFile(multipartFile: MultipartFile, path: String): String {
        val originFileName = multipartFile.originalFilename ?: throw ApiException(
            ResponseCode.S3_UPLOAD_FAILED,
            "originalFilename이 null입니다."
        )

        val fileName = createFileName(originFileName, path)
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentLength = multipartFile.size
        objectMetadata.contentType = multipartFile.contentType

        try {
            multipartFile.inputStream.use { inputStream ->
                // 파일 유효성 검사
                val tika = Tika()
                val detectedFile = tika.detect(multipartFile.bytes)
                require(detectedFile.startsWith("image")) { "AwsS3 : 올바른 이미지 파일을 올려주세요." }

                // 업로드
                amazonS3Client.putObject(
                    PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead)
                )
                return fileName
            }
        } catch (e: IOException) {
            throw ApiException(ResponseCode.S3_UPLOAD_FAILED)
        }
    }


    fun deleteFile(imgUrl: String) {
        if (imgUrl.startsWith(this.linkStart)) {
            val fileName: String =
                imgUrl.split("/".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()[4]
            amazonS3Client.deleteObject(DeleteObjectRequest(bucket, fileName))
        }
    }

    fun deleteAllFile(imageLinkList: List<String> = ArrayList()) {
        if (imageLinkList.isEmpty()) return

        val deleteObjectsRequest = DeleteObjectsRequest(bucket)
        val keyVersionList: MutableList<DeleteObjectsRequest.KeyVersion> = ArrayList()
        imageLinkList.forEach(Consumer<String> { imageLink: String ->
            val fileName: String =
                S3Service.DIR + imageLink.split("/".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()[4]
            keyVersionList.add(DeleteObjectsRequest.KeyVersion(fileName))
        })
        deleteObjectsRequest.keys = keyVersionList
        amazonS3Client.deleteObjects(deleteObjectsRequest)
    }

    private fun createFileName(
        fileName: String,
        path: String
    ): String { // 먼저 파일 업로드 시, 파일명을 난수화하기 위해 random으로 돌립니다.
        return path + (UUID.randomUUID().toString() + "_" + getFileExtension(fileName))
    }

    private fun getFileExtension(fileName: String): String { // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며, 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단하였습니다.
        try {
            return fileName.substring(fileName.lastIndexOf("."))
        } catch (e: StringIndexOutOfBoundsException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "잘못된 형식의 파일($fileName) 입니다."
            )
        }
    }

    fun getAllObject(): List<List<String>> {
        var objectListing: ObjectListing =
            amazonS3Client.listObjects(bucket, S3Service.DIR)

        val keyList: MutableList<List<String>> = ArrayList()

        do {
            val summaries = objectListing.objectSummaries

            val collects = summaries.stream()
                .map { s3ObjectSummary: S3ObjectSummary -> objectLink + s3ObjectSummary.key }
                .toList() // stream() -> mutableList
                .toList() // mutableList -> list
            keyList.add(collects)


            objectListing = amazonS3Client.listNextBatchOfObjects(objectListing)
        } while (objectListing.isTruncated)

        return keyList
    }

}
