package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel( description = "Represents a task sandbox file that was uploaded to S3" )
public class SingularityS3Log {
  public static final String LOG_START_S3_ATTR = "starttime";
  public static final String LOG_END_S3_ATTR = "endtime";

  private final String getUrl;
  private final String key;
  private final long lastModified;
  private final long size;
  private final String downloadUrl;
  private final Optional<Long> startTime;
  private final  Optional<Long> endTime;

  @JsonCreator
  public SingularityS3Log(@JsonProperty("getUrl") String getUrl, @JsonProperty("key") String key, @JsonProperty("lastModified") long lastModified, @JsonProperty("size") long size, @JsonProperty("downloadUrl") String downloadUrl,
                          @JsonProperty("startTime") Optional<Long> startTime, @JsonProperty("endTime") Optional<Long> endTime) {
    this.getUrl = getUrl;
    this.key = key;
    this.lastModified = lastModified;
    this.size = size;
    this.downloadUrl = downloadUrl;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @ApiModelProperty("URL to file in S3")
  public String getGetUrl() {
    return getUrl;
  }

  @ApiModelProperty("S3 key")
  public String getKey() {
    return key;
  }

  @ApiModelProperty("Last modified time")
  public long getLastModified() {
    return lastModified;
  }

  @ApiModelProperty("File size (in bytes)")
  public long getSize() {
    return size;
  }

  @ApiModelProperty("URL to file in S3 containing headers that will force file to be downloaded instead of viewed")
  public String getDownloadUrl() {
    return downloadUrl;
  }

  @ApiModelProperty("Time the log file started being written to")
  public Optional<Long> getStartTime() {
    return startTime;
  }

  @ApiModelProperty("Time the log file was finished being written to")
  public Optional<Long> getEndTime() {
    return endTime;
  }

  @Override
  public String toString() {
    return "SingularityS3Log{" +
        "getUrl='" + getUrl + '\'' +
        ", key='" + key + '\'' +
        ", lastModified=" + lastModified +
        ", size=" + size +
        ", downloadUrl='" + downloadUrl + '\'' +
        ", startTime=" + startTime +
        ", endTime=" + endTime +
        '}';
  }
}
