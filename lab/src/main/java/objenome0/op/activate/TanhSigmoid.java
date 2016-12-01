/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package objenome0.op.activate;

import objenome0.op.DiffableFunction;
import objenome0.op.Scalar;

/**
 *
 * @author thorsten
 */
public class TanhSigmoid implements DiffableFunction {

    private final DiffableFunction x;

    public TanhSigmoid(DiffableFunction x) {
        this.x = x;
    }

    @Override
    public double value() {
        return Math.tanh(x.value());
    }

    @Override
    public double partialDerive(Scalar parameter) {
        double y = value();
        return x.partialDerive(parameter) * (1 - y) * (1 + y);
    }

}