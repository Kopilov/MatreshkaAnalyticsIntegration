package ru.ardu_cris.mai.face;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;

/**
 * Модель для доступа к данным о веб-ресурсах из Swing-таблицы.
 * Код по возможности универсален, но предполагает, что первичный ключ (ID) -- первое
 * поле в таблице
 * @author aleksandr
 */
public class WebresourcesSwingTableModel extends AbstractTableModel {
	private static final String TABLENAME = "WEBRESOURCE";
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.ardu_cris.mai.l10n");

	private final List<String> columns = new ArrayList<>();
	private final List<Class> columnsTypes = new ArrayList<>();
	private final List<List<Object>> data = new ArrayList<>();
	
	List<TableModelListener> ll = new ArrayList<>();
	
	private final Connection connection;
	private static final Logger logger = Logger.getLogger(WebresourcesSwingTableModel.class.getName());
	
	public WebresourcesSwingTableModel(Connection connection) {
		this.connection = connection;
	}
	
	public void refresh() throws SQLException {
		data.clear();
		try (PreparedStatement statement = connection.prepareStatement("select * from " + TABLENAME + " order by ID")) {
			ResultSet resultSet = statement.executeQuery();
			int columnCount = resultSet.getMetaData().getColumnCount();
			columns.clear();
			for (int i = 1; i <= columnCount; i++) {
				columns.add(resultSet.getMetaData().getColumnLabel(i));
			}
			for (int i = 1; i <= columnCount; i++) {
				try {
					columnsTypes.add(Class.forName(resultSet.getMetaData().getColumnClassName(i)));
				} catch (ClassNotFoundException ex) {
					logger.log(Level.WARNING, null, ex);
					columnsTypes.add(String.class);
				}
			}
			while (resultSet.next()) {
				List<Object> row = new ArrayList<>();
				for (int i = 1; i <= columnCount; i++) {
					row.add(resultSet.getObject(i));
				}
				data.add(row);
			}
		}
//		fireTableDataChanged();
	}
	
	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		return columns.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data.get(rowIndex).get(columnIndex);
	}

	@Override
	public String getColumnName(int columnIndex) {
		try {
			return l10n.getString("DB_FIELD_NAME_" + getColumnSysname(columnIndex));
		} catch (MissingResourceException ex) {
			return getColumnSysname(columnIndex);
		}
	}

	/**
	 * Возвращает название поля таким, как оно выглядит в БД
	 * @param columnIndex номер поля таблицы начиная с 0
	 * @return название соответствующего поля БД
	 */
	public String getColumnSysname(int columnIndex) {
		return columns.get(columnIndex);
	}

	/**
	 * Возвращает опиание поля
	 * @param columnIndex номер поля таблицы начиная с 0
	 * @return название соответствующего поля БД
	 */
	public String getColumnDescription(int columnIndex) {
		try {
			return l10n.getString("DB_FIELD_DESCRIPTION_" + getColumnSysname(columnIndex));
		} catch (MissingResourceException ex) {
			return getColumnSysname(columnIndex);
		}
	}

	/**
	 * Возвращает всплывающую подсказку к полю
	 * @param columnIndex номер поля таблицы начиная с 0
	 * @return 
	 */
	String getColumnTip(int columnIndex) {
		return getColumnDescription(columnIndex) + " (" + getColumnSysname(columnIndex) + ")";
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnsTypes.get(columnIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex > 0; // редактировать можно всё, кроме ID
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Object id = data.get(rowIndex).get(0);
		String sql = "update " + TABLENAME + " set " + columns.get(columnIndex) + " = ? where ID = ?";
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setObject(1, aValue);
			statement.setObject(2, id);
			int updated = statement.executeUpdate();
			if (updated != 1) {
				logger.log(Level.WARNING, "{0}; -- not upated", sql);
			} else {
				data.get(rowIndex).set(columnIndex, aValue);
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

	void saveWebresourceFromForm(List<JTextField> formTextFields) throws SQLException {
		if (StringUtils.isEmpty(formTextFields.get(0).getText())) {//id == null => insert (other fields only)
			StringBuilder sqlInsert = new StringBuilder ("insert into ");
			sqlInsert.append(TABLENAME);
			sqlInsert.append(" (");
			StringBuilder sqlValues = new StringBuilder (" values (");
			boolean firstParameter = true;
			for (int i = 1; i < columns.size(); i++) {
				String stringValue = formTextFields.get(i).getText();
				if (StringUtils.isEmpty(stringValue)) {
					continue;
				}
				if (!firstParameter) {
					sqlInsert.append(", ");
					sqlValues.append(", ");
				}
				String field = columns.get(i);
				sqlInsert.append(field);
				sqlValues.append("?");
				firstParameter = false;
			}
			sqlInsert.append(")");
			sqlValues.append(")");
			String sql = sqlInsert.toString() + sqlValues.toString();
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				int sqlParam = 1;
				for (int i = 1; i < columns.size(); i++) {
					String stringValue = formTextFields.get(i).getText();
					if (StringUtils.isEmpty(stringValue)) {
						continue;
					}
					switch (columnsTypes.get(i).getName()) {
						case "java.lang.String":
							statement.setString(sqlParam, stringValue);
							break;
						case "java.lang.Integer":
							statement.setInt(sqlParam, Integer.valueOf(stringValue));
							break;
						case "java.sql.Timestamp"://in our case we have only one timestamp field set to current date
							statement.setTimestamp(sqlParam, new Timestamp(new Date().getTime()));
							break;
					}
					sqlParam++;
				}
				statement.execute();
			}
		} else {//id != null => update
			
		}
		refresh();
		fireTableDataChanged();
	}
	
	/**
	 * Удаление записей, имеющих в данный момент номера {@param selectedRows} в графической таблице
	 * @param selectedRows номера записей на удаление
	 */
	void removeRows(int[] selectedRows) {
		StringBuilder sql = new StringBuilder("delete from ");
		sql.append(TABLENAME);
		sql.append(" where ");
		sql.append(columns.get(0));
		sql.append(" in (");
		boolean firstRow = true;
		for (int rowIndex: selectedRows) {
			if (!firstRow) {
				sql.append(", ");
			}
			Object id = data.get(rowIndex).get(0);
			sql.append(id);
			firstRow = false;
		}
		sql.append(")");
		try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
			statement.execute();
			refresh();
			fireTableDataChanged();
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
}
