"use client";

import { FormEvent, useState } from "react";
import styles from "./page.module.css";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type ApiErrorBody = {
	message?: string;
	fieldErrors?: { field: string; message: string }[];
};

export default function Home() {
	const [longUrl, setLongUrl] = useState("");
	const [shortUrl, setShortUrl] = useState<string | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(false);
	const [copied, setCopied] = useState(false);

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		setError(null);
		setShortUrl(null);
		setCopied(false);
		setLoading(true);
		try {
			const res = await fetch(`${API_BASE}/api/v1/urls`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ longUrl }),
			});
			let data: ApiErrorBody & { shortUrl?: string } = {};
			try {
				data = (await res.json()) as typeof data;
			} catch {
				/* non-JSON */
			}
			if (!res.ok) {
				const baseMsg = typeof data.message === "string" ? data.message : "Request failed";
				const fieldMsg = data.fieldErrors?.map((f) => f.message).filter(Boolean).join(" ");
				setError(fieldMsg ? `${baseMsg}: ${fieldMsg}` : baseMsg);
				return;
			}
			if (typeof data.shortUrl === "string") {
				setShortUrl(data.shortUrl);
			} else {
				setError("Unexpected response from server");
			}
		} catch {
			setError("Network error. Is the API running?");
		} finally {
			setLoading(false);
		}
	}

	function copy() {
		if (!shortUrl) return;
		void navigator.clipboard.writeText(shortUrl).then(() => {
			setCopied(true);
			setTimeout(() => setCopied(false), 2000);
		});
	}

	return (
		<div className={styles.wrap}>
			<header className={styles.header}>
				<h1 className={styles.title}>URL shortener</h1>
				<p className={styles.lead}>
					Paste a long URL to get a short link that redirects to the original.
				</p>
			</header>
			<form className={styles.form} onSubmit={onSubmit}>
				<label className={styles.label} htmlFor="longUrl">
					Long URL
				</label>
				<input
					id="longUrl"
					className={styles.input}
					type="url"
					value={longUrl}
					onChange={(e) => setLongUrl(e.target.value)}
					placeholder="https://example.com/very/long/path"
					required
					disabled={loading}
					autoComplete="off"
				/>
				<button className={styles.submit} type="submit" disabled={loading}>
					{loading ? "Shortening…" : "Shorten"}
				</button>
			</form>
			{error ? <p className={styles.error}>{error}</p> : null}
			{shortUrl ? (
				<section className={styles.result} aria-live="polite">
					<p className={styles.resultLabel}>Your short link</p>
					<div className={styles.resultRow}>
						<a className={styles.shortLink} href={shortUrl} target="_blank" rel="noreferrer">
							{shortUrl}
						</a>
						<button className={styles.copy} type="button" onClick={copy}>
							{copied ? "Copied" : "Copy"}
						</button>
					</div>
				</section>
			) : null}
		</div>
	);
}
