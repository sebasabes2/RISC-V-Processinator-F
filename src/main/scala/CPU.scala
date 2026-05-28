import chisel3._
import chisel3.util._

import floatingPointUnit.Stages
import floatingPointUnit.SinglePrecisionFloatingPointUnit

class CPU extends Module {
  val io = IO(new Bundle {
    val inst = new Bus()
    val data = new Bus()
    val startAddr = Input(UInt(32.W))

    // Only for testing
    val reg = Output(Vec(32, UInt(32.W)))
    val PC = Output(UInt(32.W))
  })

  // GP-Registers
  val reg = Reg(Vec(32, UInt(32.W)))
  val newReg = WireDefault(reg)
  reg := newReg
  io.reg := newReg

  // PC
  val branch = Wire(Bool())
  val stall = Wire(Bool())
  val jumpAddress = WireInit(UInt(32.W), DontCare)
  val PC = RegInit(io.startAddr - 4.U)
  val newPC = Mux(branch, RegNext(jumpAddress), Mux(stall, PC, PC + 4.U))
  PC := newPC
  io.PC := newPC

  // Fetch
  io.inst.addr := newPC
  io.inst.read := true.B
  io.inst.writeWord := false.B
  io.inst.writeHalf := false.B
  io.inst.writeByte := false.B
  io.inst.writeData := 0.U

  // Decode
  val instruction = io.inst.readData

  val opcode = instruction(6,0) //control
  val rd = (instruction(11,7)) // write register
  val funct3 = (instruction(14,12))
  val rs1 = (instruction(19,15)) //read register 1
  val rs2 = (instruction(24,20)) //read register 2
  val funct7 = (instruction(31,25))
  
  val I_imm = Fill(20, instruction(31)) ## instruction(31,20)
  val S_imm = Fill(20, instruction(31)) ## instruction(31,25) ## instruction(11,7)
  val B_imm = Fill(19, instruction(31)) ## (instruction(31) ## instruction(7) ## instruction(30,25) ## instruction(11,8) ## 0.U(1.W))
  val U_imm = (instruction(31,12) ## 0.U(12.W))
  val J_imm = Fill(11,instruction(31)) ## (instruction(31) ## instruction(19,12) ## instruction(20) ## instruction(30,21) ## 0.U(1.W))

  // ALU signals
  val operand1 = WireInit(UInt(32.W), DontCare)
  val operand2 = WireInit(UInt(32.W), DontCare)
  val aluMode = WireDefault(0.U(4.W))
  val aluResult = WireInit(UInt(32.W), DontCare)

  // Control signals
  val exWriteBack = WireDefault(false.B)
  val memWriteBack = WireDefault(false.B)
  val memStore = WireDefault(false.B)
  val haveBranch = WireDefault(false.B)
  val jump = WireDefault(false.B)

  val useRs1 = WireDefault(false.B)
  val useRs2 = WireDefault(false.B)

  val regVal1 = Mux(RegNext(exWriteBack) && (RegNext(rd) =/= 0.U) && (RegNext(rd) === rs1), aluResult, newReg(rs1))
  val regVal2 = Mux(RegNext(exWriteBack) && (RegNext(rd) =/= 0.U) && (RegNext(rd) === rs2), aluResult, newReg(rs2))
  
  // FPU signals
  val fpuOperand1 = WireInit(UInt(32.W), DontCare)
  val fpuOperand2 = WireInit(UInt(32.W), DontCare)
  val fpuOperation = WireInit(UInt(2.W), DontCare)
  val fpuWriteBack = WireDefault(false.B)

  switch (opcode) {
    is (Opcodes.add) {
      operand1 := regVal1
      operand2 := regVal2
      exWriteBack := true.B
      aluMode := funct7(5) ## funct3
      useRs1 := true.B
      useRs2 := true.B
    }
    is (Opcodes.addi) {
      operand1 := regVal1
      operand2 := I_imm
      exWriteBack := true.B
      val artithmeticToggle = Mux(funct3 === ALUModes.shiftRight, funct7(5), 0.U(1.W))
      aluMode := artithmeticToggle ## funct3
      useRs1 := true.B
    }
    is (Opcodes.load) {
      operand1 := regVal1
      operand2 := I_imm
      memWriteBack := true.B
      useRs1 := true.B
    }
    is (Opcodes.store) {
      operand1 := regVal1
      operand2 := S_imm
      memStore := true.B
      useRs1 := true.B
    }
    is (Opcodes.branch) {
      operand1 := regVal1
      operand2 := regVal2
      haveBranch := true.B
      useRs1 := true.B
      useRs2 := true.B
      jumpAddress := PC + B_imm
    }
    is (Opcodes.lui) {
      operand1 := 0.U
      operand2 := U_imm
      exWriteBack := true.B
    }
    is (Opcodes.auipc) {
      operand1 := PC
      operand2 := U_imm
      exWriteBack := true.B
    }
    is (Opcodes.jal){
      operand1 := PC
      operand2 := 4.U
      exWriteBack := true.B
      jumpAddress := PC + J_imm
      jump := true.B
    }
    is (Opcodes.jalr){
      operand1 := PC
      operand2 := 4.U
      exWriteBack := true.B
      jumpAddress := regVal1 + I_imm
      useRs1 := true.B
      jump := true.B
    }
    is (Opcodes.fadd_s) {
      fpuOperand1 := regVal1
      fpuOperand2 := regVal2
      fpuWriteBack := true.B
      fpuOperation := funct7(3,2)
    }
  }

  stall := RegNext(memWriteBack && (rd =/= 0.U)) && (((RegNext(rd) === rs1) && useRs1) || ((RegNext(rd) === rs2) && useRs2))

  val flush = branch || stall || RegNext(reset.asBool)
  when (flush) {
    exWriteBack := false.B
    memWriteBack := false.B
    memStore := false.B
    haveBranch := false.B
    jump := false.B
  }

  // Execute
  val operand1Reg = RegNext(operand1)
  val operand2Reg = RegNext(operand2)
  val aluModeReg = RegNext(aluMode)

  switch (aluModeReg(2,0)) {
    is (ALUModes.add) {
      when (aluModeReg(3)) {
        aluResult := operand1Reg - operand2Reg
      } .otherwise {
        aluResult := operand1Reg + operand2Reg
      }
    }
    is (ALUModes.shiftLeft){
      aluResult := operand1Reg << operand2Reg(5,0)
    }
    is (ALUModes.setLessThan){
      aluResult := (operand1Reg.asSInt < operand2Reg.asSInt).asUInt
    }
    is (ALUModes.setLessThanU){
      aluResult := (operand1Reg < operand2Reg)
    }
    is (ALUModes.xor){
      aluResult := operand1Reg ^ operand2Reg
    }
    is (ALUModes.shiftRight){
      when (aluModeReg(3)) { //sra
        aluResult := (operand1Reg.asSInt >> operand2Reg(4,0)).asUInt
      } .otherwise { //srl
        aluResult := operand1Reg >> operand2Reg(4,0)
      }
    }
    is (ALUModes.or){
      aluResult := operand1Reg | operand2Reg
    }
    is (ALUModes.and){
      aluResult := operand1Reg & operand2Reg
    }
  }

  val branchTaken = WireInit(Bool(), DontCare)
  switch(RegNext(funct3)) {
    is(BranchModes.beq) {
      branchTaken := operand1Reg === operand2Reg
    }
    is(BranchModes.bne) {
      branchTaken := operand1Reg =/= operand2Reg
    }
    is(BranchModes.blt) {
      branchTaken := operand1Reg.asSInt < operand2Reg.asSInt
    }
    is(BranchModes.bge) {
      branchTaken := operand1Reg.asSInt >= operand2Reg.asSInt
    }
    is(BranchModes.bltu) {
      branchTaken := operand1Reg < operand2Reg
    }
    is(BranchModes.bgeu) {
      branchTaken := operand1Reg >= operand2Reg
    }
  }
  branch := (RegNext(haveBranch) && branchTaken) || RegNext(jump)

  io.data.addr := aluResult
  io.data.read := RegNext(memWriteBack)
  io.data.writeData := newReg(RegNext(rs2))
  io.data.writeByte := RegNext(memStore) && (RegNext(funct3) === 0.U)
  io.data.writeHalf := RegNext(memStore) && (RegNext(funct3) === 1.U)
  io.data.writeWord := RegNext(memStore) && (RegNext(funct3) === 2.U)

  val fpuStages = new Stages {
    this.output = true
    this.combiner = true
    this.shortener = true
  }
  val fpu = Module(new SinglePrecisionFloatingPointUnit(fpuStages))
  fpu.io.input1 := RegNext(fpuOperand1)
  fpu.io.input2 := RegNext(fpuOperand2)
  fpu.io.operation := RegNext(fpuOperation)
  fpu.io.roundingMode := 0.U

  // Memory/Writeback
  val loadToMem = WireInit(UInt(32.W), DontCare)
  switch(RegNext(RegNext(funct3))){
    is(0.U){ // byte
      loadToMem := Fill(24,io.data.readData(7)) ## io.data.readData(7,0)
    }
    is(1.U){ // half word
      loadToMem := Fill(16,io.data.readData(15)) ## io.data.readData(15,0)
    }
    is(2.U){ // word
      loadToMem := io.data.readData
    }
    is(4.U){ // byte unsigned
      loadToMem := io.data.readData(7,0)
    }
    is(5.U){ // half word unsigned
      loadToMem := io.data.readData(15,0)
    }
  }

  val destination = RegNext(RegNext(rd))
  when (destination =/= 0.U) {
    when (RegNext(RegNext(exWriteBack))) {
      newReg(destination) := RegNext(aluResult)
    } .elsewhen (RegNext(RegNext(memWriteBack))) {
      newReg(destination) := loadToMem
    } .elsewhen (RegNext(RegNext(RegNext(RegNext(fpuWriteBack))))) {
      newReg(destination) := fpu.io.output
    }
  }
}
