package com.ecom.repository;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.ecom.model.Product;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;

@Repository
public class ElasticSearchQuery {

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	private final String indexName = "product";

	public String createOrUpdateDocument(Product product) throws IOException {

		String productId = String.valueOf(product.getId());

		// Save to Elasticsearch
		IndexResponse response = elasticsearchClient.index(i -> i.index(indexName).id(productId).document(product));

		if (response.result().name().equals("Created")) {
			return "Document has been successfully created in both Elasticsearch and MySQL.";
		} else if (response.result().name().equals("Updated")) {
			return "Document has been successfully updated in both Elasticsearch and MySQL.";
		}
		return "Error while performing the operation.";
	}

}
