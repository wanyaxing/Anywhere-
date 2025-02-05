package com.absinthe.anywhere_.api

import com.absinthe.anywhere_.model.cloud.GitHubApiContentBean
import com.absinthe.anywhere_.model.cloud.GiteeApiContentBean
import com.absinthe.anywhere_.model.cloud.RuleEntity
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApi {

  @GET("contents/rules")
  fun requestAllContents(): Call<List<GitHubApiContentBean>>

  @GET("contents/rules")
  fun requestGiteeAllContents(): Call<List<GiteeApiContentBean>>

  @GET("{path}")
  fun requestEntity(@Path("path") path: String): Call<RuleEntity>
}
