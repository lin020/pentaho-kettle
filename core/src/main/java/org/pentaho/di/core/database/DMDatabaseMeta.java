package org.pentaho.di.core.database;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.plugins.DatabaseMetaPlugin;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;

import java.sql.ResultSet;

/**
 * DatabaseMeta数据库插件-达梦数据库
 */
// @DatabaseMetaPlugin(type = "DM", typeDescription = "达梦数据库")
public class DMDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {
 
	private static final String STRICT_BIGNUMBER_INTERPRETATION = "STRICT_NUMBER_38_INTERPRETATION";
 
	@Override
	public int[] getAccessTypeList() {
		return new int[] { DatabaseMeta.TYPE_ACCESS_NATIVE, DatabaseMeta.TYPE_ACCESS_JNDI };
	}
 
	@Override
	public int getDefaultDatabasePort() {
		if (getAccessType() == DatabaseMeta.TYPE_ACCESS_NATIVE) {
			return 5236;
		}
		return -1;
	}
 
	/**
	 * 当前数据库是否支持自增类型的字段
	 */
	@Override
	public boolean supportsAutoInc() {
		return true;
	}
 
	/**
	 * 获取限制读取条数的数据，追加再select语句后实现限制返回的结果数
	 * @see DatabaseInterface#getLimitClause(int)
	 */
	@Override
	public String getLimitClause(int nrRows) {
		return " WHERE ROWNUM <= " + nrRows;
	}
 
	/**
	 * 返回获取表所有字段信息的语句
	 * @param tableName 
	 * @return The SQL to launch.
	 */
	@Override
	public String getSQLQueryFields(String tableName) {
		return "SELECT * FROM " + tableName + " WHERE 1=0";
	}
 
	@Override
	public String getSQLTableExists(String tablename) {
		return getSQLQueryFields(tablename);
	}
 
	@Override
	public String getSQLColumnExists(String columnname, String tablename) {
		return getSQLQueryColumnFields(columnname, tablename);
	}
 
	public String getSQLQueryColumnFields(String columnname, String tableName) {
		return "SELECT " + columnname + " FROM " + tableName + " WHERE 1=0";
	}
 
	@Override
	public boolean needsToLockAllTables() {
		return false;
	}
 
	@Override
	public String getDriverClass() {
		if (getAccessType() == DatabaseMeta.TYPE_ACCESS_ODBC) {
			return "sun.jdbc.odbc.JdbcOdbcDriver";
		} else {
			return "dm.jdbc.driver.DmDriver";
		}
	}
 
	@Override
	public String getURL(String hostname, String port, String databaseName) throws KettleDatabaseException {
		if (getAccessType() == DatabaseMeta.TYPE_ACCESS_ODBC) {
			return "jdbc:odbc:" + databaseName;
		} else if (getAccessType() == DatabaseMeta.TYPE_ACCESS_NATIVE) {
			// <host>/<database>
			// <host>:<port>/<database>
			String _hostname = hostname;
			String _port = port;
			String _databaseName = databaseName;
			if (Utils.isEmpty(hostname)) {
				_hostname = "localhost";
			}
			if (Utils.isEmpty(port) || port.equals("-1")) {
				_port = "";
			}
			if (Utils.isEmpty(databaseName)) {
				throw new KettleDatabaseException("必须指定数据库名称");
			}
			if (!databaseName.startsWith("/")) {
				_databaseName = "/" + databaseName;
			}
			return "jdbc:dm://" + _hostname + (Utils.isEmpty(_port) ? "" : ":" + _port) + _databaseName;
		} else {
			throw new KettleDatabaseException("不支持的数据库连接方式[" + getAccessType() + "]");
		}
	}
 
	/**
	 * Oracle doesn't support options in the URL, we need to put these in a
	 * Properties object at connection time...
	 */
	@Override
	public boolean supportsOptionsInURL() {
		return false;
	}
 
	/**
	 * @return true if the database supports sequences
	 */
	@Override
	public boolean supportsSequences() {
		return true;
	}
 
	/**
	 * Check if a sequence exists.
	 *
	 * @param sequenceName
	 *            The sequence to check
	 * @return The SQL to get the name of the sequence back from the databases data
	 *         dictionary
	 */
	@Override
	public String getSQLSequenceExists(String sequenceName) {
		int dotPos = sequenceName.indexOf('.');
		String sql = "";
		if (dotPos == -1) {
			// if schema is not specified try to get sequence which belongs to current user
			sql = "SELECT * FROM USER_SEQUENCES WHERE SEQUENCE_NAME = '" + sequenceName.toUpperCase() + "'";
		} else {
			String schemaName = sequenceName.substring(0, dotPos);
			String seqName = sequenceName.substring(dotPos + 1);
			sql = "SELECT * FROM ALL_SEQUENCES WHERE SEQUENCE_NAME = '" + seqName.toUpperCase()
					+ "' AND SEQUENCE_OWNER = '" + schemaName.toUpperCase() + "'";
		}
		return sql;
	}
 
	/**
	 * Get the current value of a database sequence
	 *
	 * @param sequenceName
	 *            The sequence to check
	 * @return The current value of a database sequence
	 */
	@Override
	public String getSQLCurrentSequenceValue(String sequenceName) {
		return "SELECT " + sequenceName + ".currval FROM DUAL";
	}
 
	/**
	 * Get the SQL to get the next value of a sequence. (Oracle only)
	 *
	 * @param sequenceName
	 *            The sequence name
	 * @return the SQL to get the next value of a sequence. (Oracle only)
	 */
	@Override
	public String getSQLNextSequenceValue(String sequenceName) {
		return "SELECT " + sequenceName + ".nextval FROM dual";
	}
 
	@Override
	public boolean supportsSequenceNoMaxValueOption() {
		return true;
	}
 
	/**
	 * @return true if we need to supply the schema-name to getTables in order to
	 *         get a correct list of items.
	 */
	@Override
	public boolean useSchemaNameForTableList() {
		return true;
	}
 
	/**
	 * @return true if the database supports synonyms
	 */
	@Override
	public boolean supportsSynonyms() {
		return true;
	}
 
	/**
	 * Generates the SQL statement to add a column to the specified table
	 *
	 * @param tablename
	 *            The table to add
	 * @param v
	 *            The column defined as a value
	 * @param tk
	 *            the name of the technical key field
	 * @param use_autoinc
	 *            whether or not this field uses auto increment
	 * @param pk
	 *            the name of the primary key field
	 * @param semicolon
	 *            whether or not to add a semi-colon behind the statement.
	 * @return the SQL statement to add a column to the specified table
	 */
	@Override
	public String getAddColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
			String pk, boolean semicolon) {
		return "ALTER TABLE " + tablename + " ADD " + getFieldDefinition(v, tk, pk, use_autoinc, true, false);
	}
 
	/**
	 * Generates the SQL statement to drop a column from the specified table
	 *
	 * @param tablename
	 *            The table to add
	 * @param v
	 *            The column defined as a value
	 * @param tk
	 *            the name of the technical key field
	 * @param use_autoinc
	 *            whether or not this field uses auto increment
	 * @param pk
	 *            the name of the primary key field
	 * @param semicolon
	 *            whether or not to add a semi-colon behind the statement.
	 * @return the SQL statement to drop a column from the specified table
	 */
	@Override
	public String getDropColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
			String pk, boolean semicolon) {
		return "ALTER TABLE " + tablename + " DROP COLUMN " + v.getName() + Const.CR;
	}
 
	/**
	 * Generates the SQL statement to modify a column in the specified table
	 *
	 * @param tablename
	 *            The table to add
	 * @param v
	 *            The column defined as a value
	 * @param tk
	 *            the name of the technical key field
	 * @param use_autoinc
	 *            whether or not this field uses auto increment
	 * @param pk
	 *            the name of the primary key field
	 * @param semicolon
	 *            whether or not to add a semi-colon behind the statement.
	 * @return the SQL statement to modify a column in the specified table
	 */
	@Override
	public String getModifyColumnStatement(String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
			String pk, boolean semicolon) {
		ValueMetaInterface tmpColumn = v.clone();
		String tmpName = v.getName();
		boolean isQuoted = tmpName.startsWith("\"") && tmpName.endsWith("\"");
		if (isQuoted) {
			// remove the quotes first.
			//
			tmpName = tmpName.substring(1, tmpName.length() - 1);
		}
 
		int threeoh = tmpName.length() >= 30 ? 30 : tmpName.length();
		tmpName = tmpName.substring(0, threeoh);
 
		tmpName += "_KTL"; // should always be shorter than 35 positions
 
		// put the quotes back if needed.
		//
		if (isQuoted) {
			tmpName = "\"" + tmpName + "\"";
		}
		tmpColumn.setName(tmpName);
 
		// Read to go.
		//
		String sql = "";
 
		// Create a new tmp column
		sql += getAddColumnStatement(tablename, tmpColumn, tk, use_autoinc, pk, semicolon) + ";" + Const.CR;
		// copy the old data over to the tmp column
		sql += "UPDATE " + tablename + " SET " + tmpColumn.getName() + "=" + v.getName() + ";" + Const.CR;
		// drop the old column
		sql += getDropColumnStatement(tablename, v, tk, use_autoinc, pk, semicolon) + ";" + Const.CR;
		// create the wanted column
		sql += getAddColumnStatement(tablename, v, tk, use_autoinc, pk, semicolon) + ";" + Const.CR;
		// copy the data from the tmp column to the wanted column (again)
		// All this to avoid the rename clause as this is not supported on all Oracle
		// versions
		sql += "UPDATE " + tablename + " SET " + v.getName() + "=" + tmpColumn.getName() + ";" + Const.CR;
		// drop the temp column
		sql += getDropColumnStatement(tablename, tmpColumn, tk, use_autoinc, pk, semicolon);
 
		return sql;
	}
 
	@Override
	public String getFieldDefinition(ValueMetaInterface v, String tk, String pk, boolean use_autoinc,
			boolean add_fieldname, boolean add_cr) {
		StringBuilder retval = new StringBuilder(128);
 
		String fieldname = v.getName();
		int length = v.getLength();
		int precision = v.getPrecision();
 
		if (add_fieldname) {
			retval.append(fieldname).append(" ");
		}
 
		int type = v.getType();
		switch (type) {
		case ValueMetaInterface.TYPE_TIMESTAMP:
			retval.append( "TIMESTAMP" );
			break;
		case ValueMetaInterface.TYPE_DATE:
			retval.append( "DATE" );
			break;
		case ValueMetaInterface.TYPE_BOOLEAN:
			retval.append("CHAR(1)");
			break;
		case ValueMetaInterface.TYPE_INTEGER:
			retval.append( "INTEGER" );
			break;
		case ValueMetaInterface.TYPE_NUMBER:
		case ValueMetaInterface.TYPE_BIGNUMBER:
			retval.append( "NUMBER" );
			if ( length > 0 ) {
				retval.append( '(' ).append( length );
				if ( precision > 0 ) {
					retval.append( ", " ).append( precision );
				}
				retval.append( ')' );
			}
			break;
		case ValueMetaInterface.TYPE_STRING:
			if ( length >= DatabaseMeta.CLOB_LENGTH ) {
				retval.append( "CLOB" );
			} else {
				if ( length == 1 ) {
					retval.append( "CHAR(1)" );
				} else if ( length > 0 && length <= getMaxVARCHARLength() ) {
					retval.append( "VARCHAR2(" ).append( length ).append( ')' );
				} else {
					if ( length <= 0 ) {
						retval.append( "VARCHAR2(2000)" ); // We don't know, so we just use the maximum...
					} else {
						retval.append( "CLOB" );
					}
				}
			}
			break;
		case ValueMetaInterface.TYPE_BINARY:
			retval.append("BLOB");
			break;
		default:
			retval.append(" UNKNOWN");
			break;
		}
 
		if (add_cr) {
			retval.append(Const.CR);
		}
 
		return retval.toString();
	}
 
	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibridge.kettle.core.database.DatabaseInterface#getReservedWords()
	 */
	@Override
	public String[] getReservedWords() {
		return new String[] {
				"ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "ARRAYLEN", "AS", "ASC", "AUDIT", "BETWEEN", "BY", "CHAR",
				"CHECK", "CLUSTER", "COLUMN", "COMMENT", "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL",
				"DEFAULT", "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE", "EXISTS", "FILE", "FLOAT", "FOR",
				"FROM", "GRANT", "GROUP", "HAVING", "IDENTIFIED", "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL",
				"INSERT", "INTEGER", "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", "LOCK", "LONG", "MAXEXTENTS", "MINUS",
				"MODE", "MODIFY", "NOAUDIT", "NOCOMPRESS", "NOT", "NOTFOUND", "NOWAIT", "NULL", "NUMBER", "OF", "OFFLINE",
				"ON", "ONLINE", "OPTION", "OR", "ORDER", "PCTFREE", "PRIOR", "PRIVILEGES", "PUBLIC", "RAW", "RENAME",
				"RESOURCE", "REVOKE", "ROW", "ROWID", "ROWLABEL", "ROWNUM", "ROWS", "SELECT", "SESSION", "SET", "SHARE",
				"SIZE", "SMALLINT", "SQLBUF", "START", "SUCCESSFUL", "SYNONYM", "SYSDATE", "TABLE", "THEN", "TO",
				"TRIGGER", "UID", "UNION", "UNIQUE", "UPDATE", "USER", "VALIDATE", "VALUES", "VARCHAR", "VARCHAR2",
				"VIEW", "WHENEVER", "WHERE", "WITH", "PATH", "TRXID", "XML" };
	}
 
	/**
	 * @return The SQL on this database to get a list of stored procedures.
	 */
	@Override
	public String getSQLListOfProcedures() {
		/*
		 * return
		 * "SELECT DISTINCT DECODE(package_name, NULL, '', package_name||'.') || object_name "
		 * + "FROM user_arguments " + "ORDER BY 1";
		 */
		return "SELECT name FROM ORM_FUNCTIONS union SELECT name FROM ORM_PROCEDURES";
	}
 
	@Override
	public String getSQLLockTables(String[] tableNames) {
		StringBuilder sql = new StringBuilder(128);
		for (int i = 0; i < tableNames.length; i++) {
			sql.append("LOCK TABLE ").append(tableNames[i]).append(" IN EXCLUSIVE MODE;").append(Const.CR);
		}
		return sql.toString();
	}
 
	@Override
	public String getSQLUnlockTables(String[] tableNames) {
		return null; // commit handles the unlocking!
	}
 
	/**
	 * @return extra help text on the supported options on the selected database
	 *         platform.
	 */
	@Override
	public String getExtraOptionsHelpText() {
		return "https://eco.dameng.com/document/dm/zh-cn/pm/";
	}
 
	@Override
	public String[] getUsedLibraries() {
		return new String[] { "Dm7JdbcDriver15.jar", "Dm7JdbcDriver16.jar", "Dm7JdbcDriver17.jar", "Dm7JdbcDriver18.jar" };
	}
 
	/**
	 * Verifies on the specified database connection if an index exists on the
	 * fields with the specified name.
	 *
	 * @param database
	 *            a connected database
	 * @param schemaName
	 * @param tableName
	 * @param idx_fields
	 * @return true if the index exists, false if it doesn't.
	 * @throws KettleDatabaseException
	 */
	@Override
	public boolean checkIndexExists(Database database, String schemaName, String tableName, String[] idx_fields)
			throws KettleDatabaseException {
 
		String tablename = database.getDatabaseMeta().getQuotedSchemaTableCombination(schemaName, tableName);
 
		boolean[] exists = new boolean[idx_fields.length];
		for (int i = 0; i < exists.length; i++) {
			exists[i] = false;
		}
 
		try {
			//
			// Get the info from the data dictionary...
			//
			String sql = "SELECT * FROM USER_IND_COLUMNS WHERE TABLE_NAME = '" + tableName + "'";
			ResultSet res = null;
			try {
				res = database.openQuery(sql);
				if (res != null) {
					Object[] row = database.getRow(res);
					while (row != null) {
						String column = database.getReturnRowMeta().getString(row, "COLUMN_NAME", "");
						int idx = Const.indexOfString(column, idx_fields);
						if (idx >= 0) {
							exists[idx] = true;
						}
 
						row = database.getRow(res);
					}
 
				} else {
					return false;
				}
			} finally {
				if (res != null) {
					database.closeQuery(res);
				}
			}
 
			// See if all the fields are indexed...
			boolean all = true;
			for (int i = 0; i < exists.length && all; i++) {
				if (!exists[i]) {
					all = false;
				}
			}
 
			return all;
		} catch (Exception e) {
			throw new KettleDatabaseException("Unable to determine if indexes exists on table [" + tablename + "]", e);
		}
	}
 
	@Override
	public boolean requiresCreateTablePrimaryKeyAppend() {
		return true;
	}
 
	/**
	 * Most databases allow you to retrieve result metadata by preparing a SELECT
	 * statement.
	 *
	 * @return true if the database supports retrieval of query metadata from a
	 *         prepared statement. False if the query needs to be executed first.
	 */
	@Override
	public boolean supportsPreparedStatementMetadataRetrieval() {
		return false;
	}
 
	/**
	 * @return The maximum number of columns in a database, <=0 means: no known
	 *         limit
	 */
	@Override
	public int getMaxColumnsInIndex() {
		return 32;
	}
 
	/**
	 * @return The SQL on this database to get a list of sequences.
	 */
	@Override
	public String getSQLListOfSequences() {
		return "SELECT SEQUENCE_NAME FROM all_sequences";
	}
 
	/**
	 * @param string
	 * @return A string that is properly quoted for use in an Oracle SQL statement
	 *         (insert, update, delete, etc)
	 */
	@Override
	public String quoteSQLString(String string) {
		string = string.replaceAll("'", "''");
		string = string.replaceAll("\\n", "'||chr(13)||'");
		string = string.replaceAll("\\r", "'||chr(10)||'");
		return "'" + string + "'";
	}
 
	/**
	 * Returns a false as Oracle does not allow for the releasing of savepoints.
	 */
	@Override
	public boolean releaseSavepoint() {
		return false;
	}
 
	@Override
	public boolean supportsErrorHandlingOnBatchUpdates() {
		return false;
	}
 
	/**
	 * @return true if Kettle can create a repository on this type of database.
	 */
	@Override
	public boolean supportsRepository() {
		return true;
	}
 
	@Override
	public int getMaxVARCHARLength() {
		return 2000;
	}
 
	/**
	 * Oracle does not support a construct like 'drop table if exists', which is
	 * apparently legal syntax in many other RDBMSs. So we need to implement the
	 * same behavior and avoid throwing 'table does not exist' exception.
	 *
	 * @param tableName
	 *            Name of the table to drop
	 * @return 'drop table if exists'-like statement for Oracle
	 */
	@Override
	public String getDropTableIfExistsStatement(String tableName) {
		return "DECLARE num NUMBER; " +
				"BEGIN SELECT COUNT(1) INTO num FROM USER_TABLES WHERE TABLE_NAME = UPPER('" + tableName +
				"'); IF num > 0 THEN EXECUTE IMMEDIATE 'DROP TABLE " + tableName + "'; END IF; END;";
	}
 
	@Override
	public SqlScriptParser createSqlScriptParser() {
		return new SqlScriptParser(false);
	}
 
	/**
	 * @return true if using strict number(38) interpretation
	 */
	public boolean strictBigNumberInterpretation() {
		return "Y".equalsIgnoreCase(getAttributes().getProperty(STRICT_BIGNUMBER_INTERPRETATION, "N"));
	}
 
	/**
	 * @param strictBigNumberInterpretation
	 *            true if use strict number(38) interpretation
	 */
	public void setStrictBigNumberInterpretation(boolean strictBigNumberInterpretation) {
		getAttributes().setProperty(STRICT_BIGNUMBER_INTERPRETATION, strictBigNumberInterpretation ? "Y" : "N");
	}
}