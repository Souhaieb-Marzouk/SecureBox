package io.securebox.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.securebox.core.data.device.AndroidDeviceModeDetector
import io.securebox.core.data.network.AnomalyRepositoryImpl
import io.securebox.core.data.network.AndroidPortRepository
import io.securebox.core.data.network.AndroidTrafficRepository
import io.securebox.core.data.network.FirewallRepositoryImpl
import io.securebox.core.data.permission.AndroidPlatformPermissions
import io.securebox.core.data.settings.SettingsRepositoryImpl
import io.securebox.core.data.settings.secureBoxDataStore
import io.securebox.core.domain.device.DeviceModeDetector
import io.securebox.core.domain.network.AnomalyRepository
import io.securebox.core.domain.network.FirewallRepository
import io.securebox.core.domain.network.PortRepository
import io.securebox.core.domain.network.TrafficRepository
import io.securebox.core.domain.permission.PlatformPermissions
import io.securebox.core.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.secureBoxDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDeviceModeDetector(impl: AndroidDeviceModeDetector): DeviceModeDetector

    @Binds
    @Singleton
    abstract fun bindPlatformPermissions(impl: AndroidPlatformPermissions): PlatformPermissions

    @Binds
    @Singleton
    abstract fun bindTrafficRepository(impl: AndroidTrafficRepository): TrafficRepository

    @Binds
    @Singleton
    abstract fun bindPortRepository(impl: AndroidPortRepository): PortRepository

    @Binds
    @Singleton
    abstract fun bindFirewallRepository(impl: FirewallRepositoryImpl): FirewallRepository

    @Binds
    @Singleton
    abstract fun bindAnomalyRepository(impl: AnomalyRepositoryImpl): AnomalyRepository
}
