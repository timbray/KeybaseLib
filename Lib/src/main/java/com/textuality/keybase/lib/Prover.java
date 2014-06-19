package com.textuality.keybase.lib;

public interface Prover {
    public boolean prove(Proof proof);
    public String[] getProofLog();
}
