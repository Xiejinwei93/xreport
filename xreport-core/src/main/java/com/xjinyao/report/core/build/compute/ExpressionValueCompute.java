package com.xjinyao.report.core.build.compute;

import com.xjinyao.report.core.build.BindData;
import com.xjinyao.report.core.build.Context;
import com.xjinyao.report.core.definition.value.ExpressionValue;
import com.xjinyao.report.core.definition.value.ValueType;
import com.xjinyao.report.core.exception.ReportComputeException;
import com.xjinyao.report.core.expression.ExpressionUtils;
import com.xjinyao.report.core.expression.function.Function;
import com.xjinyao.report.core.expression.function.page.PageFunction;
import com.xjinyao.report.core.expression.model.Expression;
import com.xjinyao.report.core.expression.model.data.BindDataListExpressionData;
import com.xjinyao.report.core.expression.model.data.ExpressionData;
import com.xjinyao.report.core.expression.model.expr.BaseExpression;
import com.xjinyao.report.core.expression.model.expr.FunctionExpression;
import com.xjinyao.report.core.expression.model.expr.JoinExpression;
import com.xjinyao.report.core.expression.model.expr.ifelse.*;
import com.xjinyao.report.core.expression.model.expr.ifelse.*;
import com.xjinyao.report.core.model.Cell;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 谢进伟
 * @since 2023年03月01日
 */
public class ExpressionValueCompute implements ValueCompute {
	@Override
	public List<BindData> compute(Cell cell, Context context) {
		ExpressionValue exprValue = (ExpressionValue) cell.getValue();
		Expression expr = exprValue.getExpression();
		List<BindData> list = new ArrayList<>();
		if (!context.isDoPaging()) {
			boolean hasPageFun = hasPageFunction(expr);
			if (hasPageFun) {
				cell.setExistPageFunction(true);
				context.addExistPageFunctionCells(cell);
				return list;
			}
		}
		ExpressionData<?> data = expr.execute(cell, cell, context);
		if (data instanceof BindDataListExpressionData) {
			BindDataListExpressionData exprData = (BindDataListExpressionData) data;
			return exprData.getData();
		}
		Object obj = data.getData();
		if (obj instanceof List) {
			List<?> listData = (List<?>) obj;
			for (Object o : listData) {
				list.add(new BindData(o));
			}
		} else {
			if (obj != null) {
				list.add(new BindData(obj));
			}
		}
		return list;
	}

	private boolean hasPageFunction(Expression expr) {
		if (expr == null) {
			return false;
		}
		if (expr instanceof IfExpression) {
			boolean has = false;
			IfExpression ifExpr = (IfExpression) expr;
			ExpressionConditionList exprConditionList = ifExpr.getConditionList();
			if (exprConditionList != null) {
				List<ExpressionCondition> conditionList = exprConditionList.getConditions();
				if (conditionList != null) {
					for (ExpressionCondition exprCondition : conditionList) {
						Expression leftExpression = exprCondition.getLeft();
						has = hasPageFunction(leftExpression);
						if (has) {
							return has;
						}
						Expression rightExpression = exprCondition.getRight();
						has = hasPageFunction(rightExpression);
						if (has) {
							return has;
						}
					}
				}
			}
			has = hasPageFunction(ifExpr.getExpression());
			if (has) {
				return has;
			}
			ElseExpression elseExpr = ifExpr.getElseExpression();
			if (elseExpr != null) {
				has = hasPageFunction(elseExpr.getExpression());
				if (has) {
					return has;
				}
			}
			List<ElseIfExpression> elseIfList = ifExpr.getElseIfExpressions();
			if (elseIfList == null || elseIfList.size() == 0) {
				return false;
			}
			for (ElseIfExpression elseIfExpr : elseIfList) {
				has = hasPageFunction(elseIfExpr.getExpression());
				if (has) {
					return has;
				}
			}
		} else if (expr instanceof JoinExpression) {
			JoinExpression joinExpr = (JoinExpression) expr;
			List<BaseExpression> list = joinExpr.getExpressions();
			if (list == null || list.size() == 0) {
				return false;
			}
			for (BaseExpression baseExpr : list) {
				boolean has = hasPageFunction(baseExpr);
				if (has) {
					return has;
				}
			}
		} else if (expr instanceof FunctionExpression) {
			FunctionExpression funExpr = (FunctionExpression) expr;
			String name = funExpr.getName();
			Function fun = ExpressionUtils.getFunctions().get(name);
			if (fun == null) {
				throw new ReportComputeException("Function [" + name + "] not exist.");
			}
			if (fun instanceof PageFunction) {
				return true;
			}
			List<BaseExpression> list = funExpr.getExpressions();
			if (list == null || list.size() == 0) {
				return false;
			}
			for (BaseExpression baseExpr : list) {
				boolean has = hasPageFunction(baseExpr);
				if (has) {
					return has;
				}
			}
		}
		return false;
	}

	@Override
	public ValueType type() {
		return ValueType.expression;
	}
}
