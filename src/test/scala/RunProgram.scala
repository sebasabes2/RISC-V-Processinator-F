import chisel3._
import chiseltest._

object RunProgram {
  def apply(dut: CPU, program: Array[Long]) {
    var addr: BigInt = 0
    var index: BigInt = 0
    dut.reset.poke(true.B)
    dut.clock.step(1)
    addr = dut.io.inst.addr.peekInt()
    index = addr / 4
    dut.clock.step(1)
    dut.io.inst.readData.poke(program(index.intValue))
    dut.reset.poke(false.B)
    var i = 0
    while (true) {
      addr = dut.io.inst.addr.peekInt()
      index = addr / 4
      dut.clock.step(1)
      if (index >= program.length) {
        // Finish remaining stages
        dut.io.inst.readData.poke(0x00000013.U) // nop
        dut.clock.step(5)
        return
      }
      dut.io.inst.readData.poke(program(index.intValue))
      // If program is stuck
      i = i + 1
      if (i >= 10000) {
        println("Reached max clock cycles of " + i)
        return
      }
    }
  }

  def apply(dut: CPU, program: Array[Int]) {
    var longProgram: Array[Long] = program.map(x => x.toLong)
    apply(dut, longProgram)
  }
}
