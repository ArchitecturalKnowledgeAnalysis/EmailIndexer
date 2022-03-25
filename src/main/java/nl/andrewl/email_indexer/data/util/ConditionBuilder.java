package nl.andrewl.email_indexer.data.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for building SQL condition strings.
 */
public class ConditionBuilder {
	public enum ExpressionType {
		AND(" AND "),
		OR(" OR ");
		public final String delimiter;
		ExpressionType(String delimiter) {
			this.delimiter = delimiter;
		}
	}

	public enum ClauseType {
		WHERE("WHERE "),
		HAVING("HAVING ");
		public final String value;
		ClauseType(String value) {
			this.value = value;
		}
	}

	private final ClauseType clauseType;
	private final ExpressionType expressionType;
	private final List<String> conditions;

	public ConditionBuilder(ClauseType clauseType, ExpressionType expressionType) {
		this.clauseType = clauseType;
		this.expressionType = expressionType;
		this.conditions = new ArrayList<>();
	}

	public static ConditionBuilder whereAnd() {
		return new ConditionBuilder(ClauseType.WHERE, ExpressionType.AND);
	}

	public static ConditionBuilder havingAnd() {
		return new ConditionBuilder(ClauseType.HAVING, ExpressionType.AND);
	}

	public ConditionBuilder with(String condition) {
		this.conditions.add(condition);
		return this;
	}

	public String build() {
		if (conditions.isEmpty()) return "";
		return clauseType.value + String.join(expressionType.delimiter, conditions);
	}
}
