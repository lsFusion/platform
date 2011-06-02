package platform.server.data.expr.where;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.query.CompileSource;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.where.EqualMap;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWhere;

import java.util.HashMap;
import java.util.Map;

public class EqualsWhere extends CompareWhere<EqualsWhere> {

    // public только для symmetricWhere
    public EqualsWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof ValueExpr && operator2 instanceof ValueExpr)
            return BaseUtils.hashEquals(operator1,operator2)?Where.TRUE:Where.FALSE;
        if(BaseUtils.hashEquals(operator1,operator2))
            return operator1.getWhere();
        return create(new EqualsWhere(operator1, operator2));
    }

    public EqualsWhere(KeyExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected String getCompareSource(CompileSource compile) {
        return "=";
    }

    @Override
    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String result = operator1.getWhere().isTrue()?"":op1Source + " IS NULL";
        String op2Source = operator2.getSource(compile);
        if(!operator2.getWhere().isTrue())
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + op1Source + "=" + op2Source;
        if(result.length()==0)
            return compare;
        else
            return "(" + result + " OR " + compare + ")";
    }

    @Override
    public boolean twins(TwinImmutableInterface o) {
        return (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator1) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator2) ||
               (BaseUtils.hashEquals(operator1,((EqualsWhere)o).operator2) && BaseUtils.hashEquals(operator2,((EqualsWhere)o).operator1)));
    }

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        return operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31;
    }

    protected EqualsWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new EqualsWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.EQUALS;
    }

    @Override
    public KeyEquals calculateKeyEquals() {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEquals((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEquals((KeyExpr) operator2, operator1);
        return super.calculateKeyEquals();
    }

    @Override
    public MeanClassWhere getMeanClassWhere() {
        Map<VariableClassExpr,VariableClassExpr> equals = new HashMap<VariableClassExpr, VariableClassExpr>();
        ClassExprWhere classWhere = getOperandWhere().getClassWhere();

        if(operator2 instanceof VariableClassExpr && operator1 instanceof StaticClassExpr)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExpr)operator1).getStaticClass()));
        if(operator2 instanceof VariableClassExpr && operator1 instanceof VariableClassExpr)
            equals.put((VariableClassExpr)operator1,(VariableClassExpr)operator2);
        if(operator1 instanceof VariableClassExpr && operator2 instanceof StaticClassExpr)
            classWhere = classWhere.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExpr)operator2).getStaticClass()));

        return new MeanClassWhere(classWhere, equals);
    }
    // повторяет FormulaWhere так как должен andEquals сделать
    @Override
    public ClassExprWhere calculateClassWhere() {
        MeanClassWhere meanWhere = getMeanClassWhere(); // именно так а не как Formula потому как иначе бесконечный цикл getMeanClassWheres -> MeanClassWhere.getClassWhere -> means(isFalse) и т.д. пойдет
        if(operator1 instanceof VariableClassExpr && operator2 instanceof VariableClassExpr) {
            assert meanWhere.equals.size()==1;
            EqualMap equalMap = new EqualMap(2);
            equalMap.add(operator1,operator2);
            return meanWhere.classWhere.andEquals(equalMap);
        } else {
            assert meanWhere.equals.size()==0;
            return meanWhere.classWhere;
        }
    }

}
