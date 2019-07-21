package com.github.stefvanschie.quickskript.core.psi.expression.util;

import com.github.stefvanschie.quickskript.core.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * An interface indicating that something can be removed from this expression.
 *
 * @since 0.1.0
 */
public interface Removable {

    /**
     * Removes the result of the element from this expression
     *
     * @param object the object to remove
     * @since 0.1.0
     */
    void remove(@NotNull PsiElement<?> object);

}