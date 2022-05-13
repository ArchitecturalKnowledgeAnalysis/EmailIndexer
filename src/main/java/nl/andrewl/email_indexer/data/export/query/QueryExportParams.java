package nl.andrewl.email_indexer.data.export.query;

public class QueryExportParams {
	private String query = "";
	private int maxResultCount = 100;
	private boolean separateEmailThreads = false;
	private int resultCount = 0;

	public String getQuery() {
		return query;
	}

	public int getMaxResultCount() {
		return maxResultCount;
	}

	public boolean isSeparateEmailThreads() {
		return separateEmailThreads;
	}

	public int getResultCount() {
		return resultCount;
	}

	public QueryExportParams withQuery(String query) {
		this.query = query;
		return this;
	}

	public QueryExportParams withMaxResultCount(int maxResultCount) {
		this.maxResultCount = maxResultCount;
		return this;
	}

	public QueryExportParams withSeparateEmailThreads(boolean separateEmailThreads) {
		this.separateEmailThreads = separateEmailThreads;
		return this;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}
}
