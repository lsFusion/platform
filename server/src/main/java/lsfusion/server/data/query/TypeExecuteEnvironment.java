package lsfusion.server.data.query;

public enum TypeExecuteEnvironment {
    MATERIALIZE, DISABLENESTLOOP, NONE;

    public static TypeExecuteEnvironment get(Integer id) {
        if(id == null)
            return null;
        switch (id) {
            case 2:
                return MATERIALIZE;
            case 1:
                return DISABLENESTLOOP;
            case 0:
                return NONE;
        }
        return NONE;
    }

    public DynamicExecuteEnvironment create() {
        switch (this) {
            case MATERIALIZE:
                return new AdjustMaterializedExecuteEnvironment();
            case DISABLENESTLOOP:
                return new AdjustVolatileExecuteEnvironment();
            case NONE:
                return DynamicExecuteEnvironment.DEFAULT;
        }
        throw new UnsupportedOperationException();
    }
}
