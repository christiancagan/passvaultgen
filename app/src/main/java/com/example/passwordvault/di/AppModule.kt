package com.example.passwordvault.di

import android.content.Context
import com.example.passwordvault.data.database.VaultDatabase
import com.example.passwordvault.data.database.VaultEntryDao
import com.example.passwordvault.data.repository.SettingsStore
import com.example.passwordvault.data.repository.AuthAttemptStore
import com.example.passwordvault.data.repository.VaultMetadataStore
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.security.backup.AndroidBackupBase64
import com.example.passwordvault.security.backup.BackupBase64
import com.example.passwordvault.security.backup.VaultBackupCodec
import com.example.passwordvault.security.backup.VaultExporter
import com.example.passwordvault.security.backup.VaultImporter
import com.example.passwordvault.security.crypto.AesGcmCipher
import com.example.passwordvault.security.crypto.Argon2idKdf
import com.example.passwordvault.security.crypto.VaultCryptoService
import com.example.passwordvault.security.crypto.VaultSession
import com.example.passwordvault.security.keystore.SecureKeyManager
import com.example.passwordvault.security.biometric.BiometricCrypto
import com.example.passwordvault.security.biometric.BiometricManager
import com.example.passwordvault.security.password.PasswordGenerator
import com.example.passwordvault.security.password.PasswordStrengthAnalyzer
import com.example.passwordvault.security.random.SecureRandomProvider
import com.example.passwordvault.security.random.SecureRandomProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecureRandomProvider(): SecureRandomProvider = SecureRandomProviderImpl()

    @Provides
    @Singleton
    fun provideArgon2idKdf(): Argon2idKdf = Argon2idKdf()

    @Provides
    @Singleton
    fun provideAesGcmCipher(random: SecureRandomProvider): AesGcmCipher = AesGcmCipher(random)

    @Provides
    @Singleton
    fun provideVaultCryptoService(
        kdf: Argon2idKdf,
        cipher: AesGcmCipher,
        random: SecureRandomProvider,
    ): VaultCryptoService = VaultCryptoService(kdf, cipher, random)

    @Provides
    @Singleton
    fun provideSecureKeyManager(): SecureKeyManager = SecureKeyManager()

    @Provides
    @Singleton
    fun provideBiometricCrypto(keyManager: SecureKeyManager): BiometricCrypto =
        BiometricCrypto(keyManager)

    @Provides
    @Singleton
    fun provideBiometricManager(
        biometricCrypto: BiometricCrypto,
        metadataStore: VaultMetadataStore,
        session: VaultSession,
        keyManager: SecureKeyManager,
    ): BiometricManager = BiometricManager(biometricCrypto, metadataStore, session, keyManager)

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson = com.google.gson.Gson()

    @Provides
    @Singleton
    fun provideVaultBackupCodec(gson: com.google.gson.Gson): VaultBackupCodec =
        VaultBackupCodec(gson)

    @Provides
    @Singleton
    fun provideBackupBase64(): BackupBase64 = AndroidBackupBase64()

    @Provides
    @Singleton
    fun provideVaultExporter(
        cipher: AesGcmCipher,
        codec: VaultBackupCodec,
        b64: BackupBase64,
    ): VaultExporter = VaultExporter(cipher, codec, b64)

    @Provides
    @Singleton
    fun provideVaultImporter(
        kdf: Argon2idKdf,
        cipher: AesGcmCipher,
        codec: VaultBackupCodec,
        b64: BackupBase64,
    ): VaultImporter = VaultImporter(kdf, cipher, codec, b64)

    @Provides
    @Singleton
    fun provideVaultSession(): VaultSession = VaultSession()

    @Provides
    @Singleton
    fun providePasswordGenerator(random: SecureRandomProvider): PasswordGenerator = PasswordGenerator(random)

    @Provides
    @Singleton
    fun providePasswordStrengthAnalyzer(): PasswordStrengthAnalyzer = PasswordStrengthAnalyzer()

    @Provides
    @Singleton
    fun provideVaultDatabase(@ApplicationContext context: Context): VaultDatabase =
        VaultDatabase.getInstance(context)

    @Provides
    fun provideVaultEntryDao(db: VaultDatabase): VaultEntryDao = db.vaultEntryDao()

    @Provides
    @Singleton
    fun provideVaultMetadataStore(@ApplicationContext context: Context): VaultMetadataStore =
        VaultMetadataStore(context)

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)

    @Provides
    @Singleton
    fun provideAuthAttemptStore(@ApplicationContext context: Context): AuthAttemptStore =
        AuthAttemptStore(context)

    @Provides
    @Singleton
    fun provideVaultRepository(
        dao: VaultEntryDao,
        cipher: AesGcmCipher,
        session: VaultSession,
        random: SecureRandomProvider,
    ): VaultRepository = VaultRepository(dao, cipher, session, random)
}
