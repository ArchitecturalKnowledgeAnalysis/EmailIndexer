package nl.andrewl.email_indexer.gen.data.search.filter;

import nl.andrewl.email_indexer.data.search.filter.IdInFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdInFilterTest {
	@Test
	public void testGetWhereClause() {
		assertEquals("", new IdInFilter().getWhereClause());
		assertEquals("EMAIL.ID IN (1)", new IdInFilter(1L).getWhereClause());
		assertEquals("EMAIL.ID IN (1,2)", new IdInFilter(1L, 2L).getWhereClause());
	}
}
