/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.upgrade.dao.orm;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Minhchau Dang
 */
public class UpgradeOptimizedResultSetHandler implements InvocationHandler {

	public UpgradeOptimizedResultSetHandler(ResultSet resultSet)
		throws SQLException {

		_resultSet = resultSet;

		_columnNames.add(StringPool.BLANK);

		ResultSetMetaData metaData = _resultSet.getMetaData();

		int columnCount = metaData.getColumnCount();

		for (int i = 1; i <= columnCount; i++) {
			int columnType = metaData.getColumnType(i);

			_columnTypes.put(i, columnType);

			String columnName = metaData.getColumnName(i);

			_columnNames.add(columnName);

			String lowerCaseColumnName = columnName.toLowerCase();

			_columnTypes.put(lowerCaseColumnName, columnType);
		}
	}

	public Object invoke(Object proxy, Method method, Object[] arguments)
		throws Throwable {

		String methodName = method.getName();

		if (methodName.equals("next")) {
			if (_resultSet.isBeforeFirst()) {
				_next = _resultSet.next();
			}

			Object returnValue = _next;

			cacheColumnValues();

			if (_next) {
				_next = _resultSet.next();
			}

			return returnValue;
		}
		else if (methodName.equals("close")) {
			_resultSet.close();

			return null;
		}
		else {
			Object column = arguments[0];

			if (column instanceof String) {
				column = ((String) column).toLowerCase();
			}

			if (column instanceof String) {
				column = ((String) column).toLowerCase();
			}

			return _columnValues.get(column);
		}
	}

	protected void cacheColumnValues() throws SQLException, UpgradeException {
		_columnValues.clear();

		if (!_next) {
			return;
		}

		for (int i = 1; i < _columnNames.size(); ++i) {
			String columnName = _columnNames.get(i);

			String lowerCaseColumnName = columnName.toLowerCase();

			Integer columnType = _columnTypes.get(lowerCaseColumnName);

			Object value = _getValue(columnName, columnType);

			_columnValues.put(i, value);

			_columnValues.put(lowerCaseColumnName, value);
		}
	}

	private Object _getValue(String name, Integer type)
		throws SQLException, UpgradeException {

		Object value = null;

		int t = type.intValue();

		if (t == Types.BIGINT) {
			value = GetterUtil.getLong(_resultSet.getLong(name));
		}
		else if (t == Types.BIT) {
			value = GetterUtil.getBoolean(_resultSet.getBoolean(name));
		}
		else if (t == Types.BOOLEAN) {
			value = GetterUtil.getBoolean(_resultSet.getBoolean(name));
		}
		else if (t == Types.CLOB) {
			value = GetterUtil.getString(_resultSet.getString(name));
		}
		else if (t == Types.DOUBLE) {
			value = GetterUtil.getDouble(_resultSet.getDouble(name));
		}
		else if (t == Types.FLOAT) {
			value = GetterUtil.getFloat(_resultSet.getFloat(name));
		}
		else if (t == Types.INTEGER) {
			value = GetterUtil.getInteger(_resultSet.getInt(name));
		}
		else if (t == Types.SMALLINT) {
			value = GetterUtil.getShort(_resultSet.getShort(name));
		}
		else if (t == Types.TIMESTAMP) {
			value = _resultSet.getTimestamp(name);
		}
		else if (t == Types.VARCHAR) {
			value = GetterUtil.getString(_resultSet.getString(name));
		}
		else {
			throw new UpgradeException(
				"Upgrade code using unsupported class type " + type);
		}

		return value;
	}

	private static Log _log = LogFactoryUtil.getLog(
		UpgradeOptimizedResultSetHandler.class);

	private List<String> _columnNames = new ArrayList<String>();
	private Map<Object, Integer> _columnTypes = new HashMap<Object, Integer>();
	private Map<Object, Object> _columnValues = new HashMap<Object, Object>();

	private boolean _next;
	private ResultSet _resultSet;

}