package com.casshole.ui.components

/** Shared amount formatting - respects the "hide amounts" toggle (eye icon), showing a masked
 * placeholder instead of the real value everywhere money is displayed. */
fun formatCurrency(amount: Double, hidden: Boolean): String =
    if (hidden) "R$ ••••" else "R$ %.2f".format(amount)
