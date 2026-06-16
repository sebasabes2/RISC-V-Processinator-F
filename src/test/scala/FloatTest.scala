import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FloatTest extends AnyFlatSpec with ChiselScalatestTester {
  "CPU " should "pass" in {
    test(new CPU()) { dut =>
      val array = Array(
        0x3f8000b7, // lui     ra,0x3f800
        0x40000137, // lui     sp,0x40000
        0x0020f1d3, // fadd.s  ft3,ft1,ft2
        0x40a00237, // lui     tp,0x40a00
        0x0040f2d3, // fadd.s  ft5,ft1,ft4
        0x00417353, // fadd.s  ft6,ft2,ft4
        0x413003b7, // lui     t2,0x41300
        0x41500437, // lui     s0,0x41500
        0x418804b7, // lui     s1,0x41880
        0x0864f553, // fsub.s  fa0,fs1,ft6
        0x009475d3, // fadd.s  fa1,fs0,fs1
        0x10947653, // fmul.s  fa2,fs0,fs1
      )
      RunProgram(dut,array)
      dut.io.reg(1).expect(0x3F800000)
      dut.io.reg(2).expect(0x40000000)
      dut.io.reg(3).expect(0x40400000)
      dut.io.reg(4).expect(0x40A00000)
      dut.io.reg(5).expect(0x40C00000)
      dut.io.reg(6).expect(0x40E00000)
      dut.io.reg(7).expect(0x41300000)
      dut.io.reg(8).expect(0x41500000)
      dut.io.reg(9).expect(0x41880000)
      dut.io.reg(10).expect(0x41200000)
      dut.io.reg(11).expect(0x41F00000)
      dut.io.reg(12).expect(0x435D0000)
    }
  }
}
