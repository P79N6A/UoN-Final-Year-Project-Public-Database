/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.pogeyan.cmis.impl.uri.expression;

import com.pogeyan.cmis.api.uri.expression.CommonExpression;
import com.pogeyan.cmis.api.uri.expression.ExpressionParserException;
import com.pogeyan.cmis.api.uri.expression.OrderByExpression;
import com.pogeyan.cmis.api.uri.expression.SortOrder;

public class OrderByParserImpl extends FilterParserImpl implements OrderByParser {
	public OrderByParserImpl() {
		super();
	}

	@Override
	public OrderByExpression parseOrderByString(final String orderByExpression) throws ExpressionParserException {
		curExpression = orderByExpression;
		OrderByExpressionImpl orderCollection = new OrderByExpressionImpl(curExpression);

		try {
			tokenList = new Tokenizer(orderByExpression).tokenize(); // throws
																		// TokenizerMessage
		} catch (TokenizerException tokenizerException) {
			throw FilterParserExceptionImpl.createERROR_IN_TOKENIZER(tokenizerException, curExpression);
		}

		while (true) {
			CommonExpression node = null;
			try {
				CommonExpression nodeLeft = readElement(null);
				node = readElements(nodeLeft, 0);
			} catch (ExpressionParserException expressionException) {
				expressionException.setFilterTree(orderCollection);
				throw expressionException;
			} catch (ExpressionParserInternalError ex) {
				ex.printStackTrace();
			}

			OrderExpressionImpl orderNode = new OrderExpressionImpl(node);

			// read the sort order
			Token token = tokenList.lookToken();
			if (token == null) {
				orderNode.setSortOrder(SortOrder.asc);
			} else if ((token.getKind() == TokenKind.LITERAL) && ("asc".equals(token.getUriLiteral()))) {
				orderNode.setSortOrder(SortOrder.asc);
				tokenList.next();
				token = tokenList.lookToken();
			} else if ((token.getKind() == TokenKind.LITERAL) && ("desc".equals(token.getUriLiteral()))) {
				orderNode.setSortOrder(SortOrder.desc);
				tokenList.next();
				token = tokenList.lookToken();
			} else if (token.getKind() == TokenKind.COMMA) {
				orderNode.setSortOrder(SortOrder.asc);
			} else {
				// Tested with TestParserExceptions.TestOPMparseOrderByString
				// CASE 1
				throw FilterParserExceptionImpl.createINVALID_SORT_ORDER(token, curExpression);
			}

			orderCollection.addOrder(orderNode);

			// ls_token may be a ',' or empty.
			if (token == null) {
				break;
			} else if (token.getKind() == TokenKind.COMMA) {
				Token oldToken = token;
				tokenList.next();
				token = tokenList.lookToken();

				if (token == null) {
					// Tested with
					// TestParserExceptions.TestOPMparseOrderByString CASE 2
					throw FilterParserExceptionImpl.createEXPRESSION_EXPECTED_AFTER_POS(oldToken, curExpression);
				}
			} else { // e.g. in case $orderby=String asc a

				throw FilterParserExceptionImpl.createCOMMA_OR_END_EXPECTED_AT_POS(token, curExpression);
			}

		}
		return orderCollection;
	}
}
