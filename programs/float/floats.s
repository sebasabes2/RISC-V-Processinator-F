.text
.global fadd
fadd:
    fadd.s a0, a0, a1, rne
    ret
.global fsub
fsub:
    fsub.s a0, a0, a1, rne
    ret
.global fmul
fmul:
    fmul.s a0, a0, a1, rne
    ret
