package com.urlshortener.app.models.dto;

public record CreateUrlResponse(String shortCode, String shortUrl, String longUrl) {}
