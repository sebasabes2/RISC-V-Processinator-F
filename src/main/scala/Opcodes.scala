import chisel3._
import chisel3.util._

object Opcodes{
  val add    = "b0110011".U
  val addi   = "b0010011".U
  val load   = "b0000011".U
  val store  = "b0100011".U
  val branch = "b1100011".U
  val jal    = "b1101111".U
  val jalr   = "b1100111".U
  val lui    = "b0110111".U
  val auipc  = "b0010111".U
  val ecall  = "b1110011".U

  val fadd_s = "b1010011".U
}

object ALUModes {
  val add = 0x0.U
  val xor = 0x4.U
  val or = 0x6.U
  val and = 0x7.U
  val shiftLeft = 0x1.U
  val shiftRight = 0x5.U
  val setLessThan = 0x2.U
  val setLessThanU = 0x3.U
}

object BranchModes {
  val beq = 0x0.U
  val bne = 0x1.U
  val blt = 0x4.U
  val bge = 0x5.U
  val bltu = 0x6.U
  val bgeu = 0x7.U
}
