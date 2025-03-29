package com.ecom.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticSearchConfiguration {

	@Bean
	RestClient getRestClient() {
		return RestClient.builder(HttpHost.create("http://localhost:9200"))
				.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000) // Connection
																												// timeout
																												// in
																												// milliseconds
						.setSocketTimeout(60000) // Socket timeout in milliseconds
				).build();
	}

	@Bean
	ElasticsearchTransport getElasticsearchTransport() {
		return new RestClientTransport(getRestClient(), new JacksonJsonpMapper());
	}

	@Bean
	ElasticsearchClient getElasticsearchClient() {
		ElasticsearchClient client = new ElasticsearchClient(getElasticsearchTransport());
		return client;
	}

}