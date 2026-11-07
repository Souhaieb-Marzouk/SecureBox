package io.securebox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.securebox.BuildConfig
import io.securebox.core.data.security.PackageSignatureVerifier
import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.network.DetectAnomaliesUseCase
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCalculateSecurityScore(): CalculateSecurityScoreUseCase = CalculateSecurityScoreUseCase()

    @Provides
    @Singleton
    fun provideDetectAnomalies(): DetectAnomaliesUseCase = DetectAnomaliesUseCase()

    @Provides
    @Singleton
    fun provideAppVersion(): AppVersion = AppVersion(versionName = BuildConfig.VERSION_NAME, versionCode = BuildConfig.VERSION_CODE)

    /**
     * Set at build time from the release keystore. When the build is unsigned (local debug work)
     * the value is empty and the integrity check reports NOT_CONFIGURED instead of a false alarm.
     */
    @Provides
    @Singleton
    @Named(PackageSignatureVerifier.EXPECTED_SIGNING_CERT_SHA256)
    fun provideExpectedSigningCertSha256(): String = BuildConfig.EXPECTED_SIGNING_CERT_SHA256
}
