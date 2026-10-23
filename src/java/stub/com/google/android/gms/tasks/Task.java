package com.google.android.gms.tasks;
// compile-only stub
public abstract class Task<T> {
    public abstract boolean isSuccessful();
    public abstract T getResult();
    public abstract Exception getException();
    public abstract Task<T> addOnCompleteListener(OnCompleteListener<T> l);
}
