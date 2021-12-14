package minedbms.datatype;

/*
Condition class for Select. (Might also be used for condition join in future)
*/
public abstract class Condition {

        public abstract boolean testCondition(Relation relation, int tupleIdx);

        public static abstract class Inequality extends Condition {

            private Inequality(String attribute_name, String comparedValue) {
                this.attributeName = attribute_name;
                this.comparedValue = comparedValue;
            }

            private Inequality(Inequality inequality) {
                this.attributeName = inequality.attributeName;
                this.comparedValue = inequality.comparedValue;

            }

            private String attributeName;
            private String comparedValue;

            private int compareValue(Relation relation, int tupleIdx) {
                int index = relation.getAttrIdx(attributeName);
                return relation.getAttributes()[index].compare(relation.getTuple(tupleIdx)[index], comparedValue);
            }

            public static class GreaterThan extends Inequality {

                public GreaterThan(String attribute_name, String comparedValue) {
                    super(attribute_name, comparedValue);
                }

                public GreaterThan(Inequality inequality) {
                    super(inequality);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    return super.compareValue(relation, tupleIdx) > 0;
                }

            }

            public static class GreaterThanEqualTo extends Inequality {

                public GreaterThanEqualTo(String attribute_name, String comparedValue) {
                    super(attribute_name, comparedValue);
                }

                public GreaterThanEqualTo(Inequality inequality) {
                    super(inequality);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    int result = super.compareValue(relation, tupleIdx);
                    return result > 0 || result == 0;
                }
            }

            public static class EqualTo extends Inequality {

                public EqualTo(String attribute_name, String comparedValue) {
                    super(attribute_name, comparedValue);
                }

                public EqualTo(Inequality inequality) {
                    super(inequality);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    return super.compareValue(relation, tupleIdx) == 0;
                }
            }

            public static class LessThanEqualTo extends Inequality {

                public LessThanEqualTo(String attribute_name, String comparedValue) {
                    super(attribute_name, comparedValue);
                }

                public LessThanEqualTo(Inequality inequality) {
                    super(inequality);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    int result = super.compareValue(relation, tupleIdx);
                    return result < 0 || result == 0;
                }
            }

            public static class LessThan extends Inequality {

                public LessThan(String attribute_name, String comparedValue) {
                    super(attribute_name, comparedValue);
                }

                public LessThan(Inequality inequality) {
                    super(inequality);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    return super.compareValue(relation, tupleIdx) < 0;
                }
            }
        }

        public static abstract class LogicalConjunction extends Condition {

            Condition leftCondition;
            Condition rightCondition;

            private LogicalConjunction(Condition leftCondition, Condition rightCondition) {
                this.leftCondition = leftCondition;
                this.rightCondition = rightCondition;
            }

            public static class And extends LogicalConjunction {

                public And(Condition leftCondiction, Condition rightCondiction) {
                    super(leftCondiction, rightCondiction);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    return this.leftCondition.testCondition(relation, tupleIdx) && this.rightCondition.testCondition(relation, tupleIdx);
                }
            }

            public static class Or extends LogicalConjunction {

                public Or(Condition leftCondiction, Condition rightCondiction) {
                    super(leftCondiction, rightCondiction);
                }

                @Override
                public boolean testCondition(Relation relation, int tupleIdx) {
                    return this.leftCondition.testCondition(relation, tupleIdx) || this.rightCondition.testCondition(relation, tupleIdx);
                }
            }
        }
    }