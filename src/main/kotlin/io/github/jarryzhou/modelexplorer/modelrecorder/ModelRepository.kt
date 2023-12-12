package io.github.jarryzhou.modelexplorer.modelrecorder

import io.github.jarryzhou.modelexplorer.ui.Model
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.text.SimpleDateFormat

private const val HISTORY_TABLE_NAME = "model_history"
private const val TABLE_FIELD_CREATE_TIME = "create_time"
private const val TABLE_FIELD_ID = "id"
private const val SELECT_ALL_DIAGRAM_SQL = "SELECT * FROM $HISTORY_TABLE_NAME ORDER BY $TABLE_FIELD_CREATE_TIME DESC"
private const val HISTORY_TABLE_DDL =
    "CREATE TABLE IF NOT EXISTS $HISTORY_TABLE_NAME (\n    id SERIAL PRIMARY KEY,\n    create_time TIMESTAMP,\n    diagram TEXT\n);"

private const val INSERT_HISTORY_SQL = "INSERT INTO $HISTORY_TABLE_NAME (create_time, diagram) VALUES (?, ?)"

private const val CHECK_HISTORY_TABLE_SQL =
    "SELECT EXISTS (\n   SELECT 1\n   FROM   pg_tables\n   WHERE    tablename = '$HISTORY_TABLE_NAME'\n);"

private const val TABLE_FIELD_DIAGRAM = "diagram"

private const val SELECT_LAST_DIAGRAM_SQL =
    "SELECT $TABLE_FIELD_DIAGRAM FROM $HISTORY_TABLE_NAME\nORDER BY create_time DESC\nLIMIT 1"

private const val SELECT_BY_ID_SQL =
    "SELECT * FROM $HISTORY_TABLE_NAME WHERE ID = ?"

private const val CREATE_DATE_PARAMETER_INDEX = 1

private const val SVG_PARAMETER_INDEX = 2

private const val DATE_FORMAT = "dd/MM/yyyy HH:mm:ss"

private const val DELETE_MODEL_SQL = "DELETE from $HISTORY_TABLE_NAME WHERE ID = ?"

@Repository
class ModelRepository(val jdbcTemplate: JdbcTemplate) {
    fun findAllModels(): List<Model> {
        return jdbcTemplate.query(SELECT_ALL_DIAGRAM_SQL) { rs, _ ->
            Model(
                id = rs.getLong(TABLE_FIELD_ID),
                name = SimpleDateFormat(DATE_FORMAT).format(rs.getTimestamp(TABLE_FIELD_CREATE_TIME)),
                diagram = "",
                info = ""
            )
        }
    }

    fun findById(id: Long): Model? =
        jdbcTemplate.queryForObject(SELECT_BY_ID_SQL, { rs, _ ->
            Model(
                id = rs.getLong(TABLE_FIELD_ID),
                name = SimpleDateFormat(DATE_FORMAT).format(rs.getTimestamp(TABLE_FIELD_CREATE_TIME)),
                diagram = rs.getString(TABLE_FIELD_DIAGRAM),
                info = ""
            )
        }, id)

    fun deleteById(id: Long) =
        jdbcTemplate.update(DELETE_MODEL_SQL, id)
    fun doRecord(diagram: String) {
        jdbcTemplate.update(INSERT_HISTORY_SQL) { ps ->
            ps.setTimestamp(CREATE_DATE_PARAMETER_INDEX, Timestamp(System.currentTimeMillis()))
            ps.setString(SVG_PARAMETER_INDEX, diagram)
        }
    }

    fun getLastDiagram(): String? {
        val result = jdbcTemplate.query(
            SELECT_LAST_DIAGRAM_SQL,
            RowMapper { rs, _ ->
                return@RowMapper rs.getString(TABLE_FIELD_DIAGRAM)
            }
        )
        return result.firstOrNull()
    }

    private fun doesHistoryTableExist(): Boolean =
        jdbcTemplate.queryForObject(CHECK_HISTORY_TABLE_SQL, Boolean::class.java) ?: false

    fun ensureModelHistoryTable() {
        if (!doesHistoryTableExist()) {
            jdbcTemplate.execute(HISTORY_TABLE_DDL)
        }
    }
}
