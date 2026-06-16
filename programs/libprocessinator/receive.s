.text
.global receive
receive:
li t1, 0x9000 # UART address
lw t0, 4(t1)
andi t0, t0, 2
beq x0, t0, receive
lw a0, 0(t1)
jalr x0, 0(ra)
