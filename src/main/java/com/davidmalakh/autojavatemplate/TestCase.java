package com.davidmalakh.autojavatemplate;

public class TestCase {
	/*-
	* FIELDS:
	* this.a --int
	* this.b --int
	* this.c --int
	* this.l --ALower
	* METHODS:
	* this.doIt(Lower that) --double
	* METHODS FROM FIELDS:
	* this.l.doAbstractThing(int thing2) --int
	-*/
    int a;
    int b;
    int c;
    ALower l;

    public TestCase(int a, int b, int c, ALower l) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.l = l;
    }
    
    public double doIt(Lower that) {
		/*-
		* METHODS FROM PARAMS:
		* this.that.doThing(int thing1, String otherThing1, Double lastThing1) --String
		* this.that.abstractMethod(int ap) --int
		-*/
        return 0.0;
    }
}

abstract class ALower {
	/*-
	* FIELDS:
	* this.y --int
	* METHODS:
	* this.doAbstractThing(int thing2) --int
	-*/
    int y;

    public ALower(int y) {
        this.y = y;
    }
    
    public int doAbstractThing(int thing2) {
        return 0;
    }
    
    abstract int abstractMethod(int ap);
}

class Lower extends ALower {
	/*-
	* FIELDS:
	* this.x --int
	* this.b --int
	* METHODS:
	* this.doThing(int thing1, String otherThing1, Double lastThing1) --String
	* this.abstractMethod(int ap) --int
	*
	* PLUS EVERYTHING FROM SUPER CLASS:  ALower
	-*/
    int x;
    int b;

    public Lower(int x, int b, int y) {
        super(y);
        this.x = x;
        this.b = b;
    }
    
    public String doThing(int thing1, String otherThing1, Double lastThing1) {
        return "";
    }

    int abstractMethod(int ap) {
        return 0;
    }
}

class NoMethods {
	/*-
	* FIELDS:
	* this.tada --int
	-*/
    int tada;

    public NoMethods(int tada) {
        this.tada = tada;
    }
}
