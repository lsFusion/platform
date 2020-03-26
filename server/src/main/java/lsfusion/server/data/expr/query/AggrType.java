package lsfusion.server.data.expr.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public interface AggrType {

    boolean isSelect(); // оператор выбирает из множества значений (а не суммирует, объединяет и т.п.)
    boolean isSelectNotInWhere(); // в общем то оптимизационная вещь потом можно убрать

    Type getType(Type exprType);
    int getMainIndex();

    boolean canBeNull(); // может возвращать null если само выражение не null
    
    Where getWhere(ImList<Expr> exprs); // there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
    Expr getMainExpr(ImList<Expr> exprs); // there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
    ImList<Expr> followFalse(Where falseWhere, ImList<Expr> exprs, boolean pack);
}
