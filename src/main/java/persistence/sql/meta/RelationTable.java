package persistence.sql.meta;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RelationTable {

    private RelationTable() {
    }

    private static final Map<Table, Set<Map.Entry<Table, Column>>> relationTableMap = new ConcurrentHashMap<>();

    public static void setRelationTable(Table table, Columns columns) {
        columns.getRelationColumns().stream()
            .filter(Column::isOneToMany)
            .forEach(column -> updateRelationTable(table, column));
    }

    private static void updateRelationTable(Table table, Column column) {
        Table relationTable = column.getRelationTable();
        relationTableMap.computeIfAbsent(relationTable, k -> new HashSet<>()).add(Map.entry(table, column));
    }

    public static Set<Map.Entry<Table, Column>> getRelationColumns(Table table) {
        return relationTableMap.getOrDefault(table, Set.of());
    }

    public static Column getJoinColumn(Table table, Table relationTable) {
        return relationTableMap.get(relationTable).stream()
            .filter(entry -> entry.getKey().equals(table))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("조인 컬럼을 찾을 수 없습니다."));
    }
}
