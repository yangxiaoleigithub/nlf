package nc.liat6.frame.lib.org.objectweb.asm;

public class MethodAdapter implements MethodVisitor{

  protected MethodVisitor mv;

  public MethodAdapter(final MethodVisitor mv){
    this.mv = mv;
  }

  public AnnotationVisitor visitAnnotationDefault(){
    return mv.visitAnnotationDefault();
  }

  public AnnotationVisitor visitAnnotation(final String desc,final boolean visible){
    return mv.visitAnnotation(desc,visible);
  }

  public AnnotationVisitor visitParameterAnnotation(final int parameter,final String desc,final boolean visible){
    return mv.visitParameterAnnotation(parameter,desc,visible);
  }

  public void visitAttribute(final Attribute attr){
    mv.visitAttribute(attr);
  }

  public void visitCode(){
    mv.visitCode();
  }

  public void visitFrame(final int type,final int nLocal,final Object[] local,final int nStack,final Object[] stack){
    mv.visitFrame(type,nLocal,local,nStack,stack);
  }

  public void visitInsn(final int opcode){
    mv.visitInsn(opcode);
  }

  public void visitIntInsn(final int opcode,final int operand){
    mv.visitIntInsn(opcode,operand);
  }

  public void visitVarInsn(final int opcode,final int var){
    mv.visitVarInsn(opcode,var);
  }

  public void visitTypeInsn(final int opcode,final String type){
    mv.visitTypeInsn(opcode,type);
  }

  public void visitFieldInsn(final int opcode,final String owner,final String name,final String desc){
    mv.visitFieldInsn(opcode,owner,name,desc);
  }

  public void visitMethodInsn(final int opcode,final String owner,final String name,final String desc){
    mv.visitMethodInsn(opcode,owner,name,desc);
  }

  public void visitJumpInsn(final int opcode,final Label label){
    mv.visitJumpInsn(opcode,label);
  }

  public void visitLabel(final Label label){
    mv.visitLabel(label);
  }

  public void visitLdcInsn(final Object cst){
    mv.visitLdcInsn(cst);
  }

  public void visitIincInsn(final int var,final int increment){
    mv.visitIincInsn(var,increment);
  }

  public void visitTableSwitchInsn(final int min,final int max,final Label dflt,final Label[] labels){
    mv.visitTableSwitchInsn(min,max,dflt,labels);
  }

  public void visitLookupSwitchInsn(final Label dflt,final int[] keys,final Label[] labels){
    mv.visitLookupSwitchInsn(dflt,keys,labels);
  }

  public void visitMultiANewArrayInsn(final String desc,final int dims){
    mv.visitMultiANewArrayInsn(desc,dims);
  }

  public void visitTryCatchBlock(final Label start,final Label end,final Label handler,final String type){
    mv.visitTryCatchBlock(start,end,handler,type);
  }

  public void visitLocalVariable(final String name,final String desc,final String signature,final Label start,final Label end,final int index){
    mv.visitLocalVariable(name,desc,signature,start,end,index);
  }

  public void visitLineNumber(final int line,final Label start){
    mv.visitLineNumber(line,start);
  }

  public void visitMaxs(final int maxStack,final int maxLocals){
    mv.visitMaxs(maxStack,maxLocals);
  }

  public void visitEnd(){
    mv.visitEnd();
  }
}
