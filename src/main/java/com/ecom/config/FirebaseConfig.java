package com.ecom.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Configuration
public class FirebaseConfig {

	@Bean
	Storage firebaseStorage() throws IOException {
		FileInputStream serviceAccount = new FileInputStream("src/main/resources/serviceAccountKey.json");

		return StorageOptions.newBuilder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build()
				.getService();
	}
}
