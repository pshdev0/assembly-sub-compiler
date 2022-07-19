// VAR0 - address to decrease
// VAR1 - upper boundary

lda VAR0+1    		// respect the upper boundary
cmp #hi(VAR1)
bne+ {
    lda VAR0
    cmp #lo(VAR1)
    beq finish
}

inc VAR0		// move up
lda VAR0
cmp #0
bne+ {
    inc VAR0+1
}

finish:
