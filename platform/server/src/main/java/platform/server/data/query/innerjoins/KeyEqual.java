package platform.server.data.query.innerjoins;

import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.caches.TranslateContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.ExprEqualsJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;

public class KeyEqual extends TwinImmutableObject implements DNFWheres.Interface<KeyEqual>,TranslateContext<KeyEqual> {

    public final ImMap<KeyExpr, BaseExpr> keyExprs;

    private KeyEqual() {
        this.keyExprs = MapFact.EMPTY();
    }
    public final static KeyEqual EMPTY = new KeyEqual();

    public KeyEqual(KeyExpr key, BaseExpr expr) {
        keyExprs = MapFact.singleton(key, expr);
    }

    public KeyEqual(ImMap<KeyExpr, BaseExpr> keyExprs) {
        this.keyExprs = keyExprs;
    }
    
    private final static AddValue<KeyExpr, Expr> keepValue = new SimpleAddValue<KeyExpr, Expr>() {
        public Expr addValue(KeyExpr key, Expr prevValue, Expr newValue) {
            if(!prevValue.isValue()) // если было не value, предпочтительнее использовать value;
                return newValue;
            return prevValue;
        }

        public boolean symmetric() {
            return true;
        }
    };
    public static <E extends Expr> AddValue<KeyExpr, E> keepValue() {
        return (AddValue<KeyExpr, E>) keepValue;
    }

    public KeyEqual and(KeyEqual and) {
        return new KeyEqual(keyExprs.merge(and.keyExprs, KeyEqual.<BaseExpr>keepValue()));
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isEmpty() {
        return keyExprs.isEmpty();
    }

    @IdentityInstanceLazy
    public QueryTranslator getTranslator() {
        return new PartialQueryTranslator(keyExprs);
    }

    public Where getWhere() {
        Where equalsWhere = Where.TRUE;
        for(int i=0,size=keyExprs.size();i<size;i++)
            equalsWhere = equalsWhere.and(EqualsWhere.create(keyExprs.getKey(i),keyExprs.getValue(i)));
        return equalsWhere;
    }

    public boolean twins(TwinImmutableObject o) {
        return keyExprs.equals(((KeyEqual) o).keyExprs);
    }

    public int immutableHashCode() {
        return keyExprs.hashCode();
    }

    public static KeyEqual getKeyEqual(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEqual((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEqual((KeyExpr) operator2, operator1);
        return KeyEqual.EMPTY;
    }

    public WhereJoins getWhereJoins() {
        WhereJoin[] wheres = new WhereJoin[keyExprs.size()]; int iw = 0;
        for(int i=0,size=keyExprs.size();i<size;i++)
            wheres[iw++] = new ExprEqualsJoin(keyExprs.getKey(i), keyExprs.getValue(i));
        return new WhereJoins(wheres);
    }
    
    public KeyStat getKeyStat(final KeyStat keyStat) {
        return new platform.server.data.query.stat.KeyStat() {
            public Stat getKeyStat(KeyExpr key) {
                BaseExpr keyExpr = keyExprs.get(key);
                if(keyExpr!=null)
                    return keyExpr.getTypeStat(keyStat);
                else
                    return keyStat.getKeyStat(key);
            }
        };
    }

    public KeyEqual translateOuter(MapTranslate translate) {
        return new KeyEqual(translate.translateMap(keyExprs));
    }
}
