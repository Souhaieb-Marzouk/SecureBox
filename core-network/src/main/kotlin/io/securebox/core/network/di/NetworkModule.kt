package io.securebox.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.securebox.core.domain.install.ApkInstaller
import io.securebox.core.domain.repository.UpdateRepository
import io.securebox.core.network.BuildConfig
import io.securebox.core.network.github.GithubApiClient
import io.securebox.core.network.github.GithubUpdateRepository
import io.securebox.core.network.install.ApkInstallerImpl
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    @Provides
    @Singleton
    fun provideGithubApiClient(client: OkHttpClient): GithubApiClient =
        GithubApiClient(
            client = client,
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO,
            apiBase = BuildConfig.GITHUB_API_BASE,
        )
}@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {
    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: GithubUpdateRepository): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindApkInstaller(impl: ApkInstallerImpl): ApkInstaller
}
