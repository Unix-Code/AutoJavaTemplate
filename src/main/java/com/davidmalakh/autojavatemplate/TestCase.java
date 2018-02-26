package com.davidmalakh.autojavatemplate;

interface IFunc2<Arg1, Arg2, Ret> {
  Ret apply(Arg1 a1, Arg2 a2);
}

interface IFunc<Arg, Ret> {
  Ret apply(Arg a);
}

interface IArithVisitor<R> extends IFunc<IArith, R> {
  R visitConst(Const c);

  R visitFormula(Formula f);
}

interface IArith {
  <R> R accept(IArithVisitor<R> visitor);
}

class Const implements IArith {
	/*-
	* FIELDS:
	* this.num --double
	* METHODS:
	* this.accept(IArithVisitor visitor) --R
	-*/
  double num;

  Const(double num) {
    this.num = num;
  }

  public <R> R accept(IArithVisitor<R> visitor) {
    return visitor.visitConst(this);
  }
}

class Formula implements IArith {
	/*-
	* FIELDS:
	* this.fun --IFunc2
	* this.name --String
	* this.left --IArith
	* this.right --IArith
	* METHODS:
	* this.accept(IArithVisitor visitor) --R
	-*/
  IFunc2<Double, Double, Double> fun;
  String name;
  IArith left;
  IArith right;

  Formula(IFunc2<Double, Double, Double> fun, String name, IArith left, IArith right) {
    this.fun = fun;
    this.name = name;
    this.left = left;
    this.right = right;
  }

  public <R> R accept(IArithVisitor<R> visitor) {
    return visitor.visitFormula(this);
  }
}

class EvalVisitor implements IArithVisitor<Double> {
	/*-
	* METHODS:
	* this.apply(IArith a) --Double
	* this.visitConst(Const c) --Double
	* this.visitFormula(Formula f) --Double
	-*/

  public Double apply(IArith a) {
    return a.accept(this);
  }

  public Double visitConst(Const c) {
		/*-
		* METHODS FROM PARAMS:
		* this.c.accept(IArithVisitor visitor) --R
		-*/
    return c.num;
  }

  public Double visitFormula(Formula f) {
		/*-
		* METHODS FROM PARAMS:
		* this.f.accept(IArithVisitor visitor) --R
		-*/
    return f.fun.apply(this.apply(f.left), this.apply(f.right));
  }
}

class PrintVisitor implements IArithVisitor<String> {
	/*-
	* METHODS:
	* this.apply(IArith a) --String
	* this.visitConst(Const c) --String
	* this.visitFormula(Formula f) --String
	-*/

  public String apply(IArith a) {
    return a.accept(this);
  }

  public String visitConst(Const c) {
		/*-
		* METHODS FROM PARAMS:
		* this.c.accept(IArithVisitor visitor) --R
		-*/
    return Double.toString(c.num);
  }

  public String visitFormula(Formula f) {
		/*-
		* METHODS FROM PARAMS:
		* this.f.accept(IArithVisitor visitor) --R
		-*/
    return "(" + f.name + " " + this.apply(f.left) + " " + this.apply(f.right) + ")";
  }
}

class DoublerVisitor implements IArithVisitor<IArith> {
	/*-
	* METHODS:
	* this.apply(IArith a) --IArith
	* this.visitConst(Const c) --IArith
	* this.visitFormula(Formula f) --IArith
	-*/

  public IArith apply(IArith a) {
    return a.accept(this);
  }

  public IArith visitConst(Const c) {
		/*-
		* METHODS FROM PARAMS:
		* this.c.accept(IArithVisitor visitor) --R
		-*/
    return new Const(c.num * 2);
  }

  public IArith visitFormula(Formula f) {
		/*-
		* METHODS FROM PARAMS:
		* this.f.accept(IArithVisitor visitor) --R
		-*/
    return new Formula(f.fun, f.name, this.apply(f.left), this.apply(f.right));
  }
}

class AllSmallVisitor implements IArithVisitor<Boolean> {
	/*-
	* METHODS:
	* this.apply(IArith a) --Boolean
	* this.visitConst(Const c) --Boolean
	* this.visitFormula(Formula f) --Boolean
	-*/

  public Boolean apply(IArith a) {
    return a.accept(this);
  }

  public Boolean visitConst(Const c) {
		/*-
		* METHODS FROM PARAMS:
		* this.c.accept(IArithVisitor visitor) --R
		-*/
    return c.num < 10;
  }

  public Boolean visitFormula(Formula f) {
		/*-
		* METHODS FROM PARAMS:
		* this.f.accept(IArithVisitor visitor) --R
		-*/
    return this.apply(f.left) && this.apply(f.right);
  }
}

class NoDivBy0 implements IArithVisitor<Boolean> {
	/*-
	* METHODS:
	* this.apply(IArith a) --Boolean
	* this.visitConst(Const c) --Boolean
	* this.visitFormula(Formula f) --Boolean
	-*/

  public Boolean apply(IArith a) {
    return a.accept(this);
  }

  public Boolean visitConst(Const c) {
		/*-
		* METHODS FROM PARAMS:
		* this.c.accept(IArithVisitor visitor) --R
		-*/
    return true;
  }

  public Boolean visitFormula(Formula f) {
		/*-
		* METHODS FROM PARAMS:
		* this.f.accept(IArithVisitor visitor) --R
		-*/
    if (f.name.equals("div")) {
      return this.apply(f.left) && this.apply(f.right)
          && Math.abs((new EvalVisitor()).apply(f.right)) > 0.0001;
    }
    else {
      return this.apply(f.left) && this.apply(f.right);
    }
  }

}
