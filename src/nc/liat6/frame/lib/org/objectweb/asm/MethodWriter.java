package nc.liat6.frame.lib.org.objectweb.asm;

class MethodWriter implements MethodVisitor{

  static final int ACC_CONSTRUCTOR = 262144;
  static final int SAME_FRAME = 0; // to 63 (0-3f)
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)
  static final int RESERVED = 128;
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7
  static final int CHOP_FRAME = 248; // to 250 (f8-fA)
  static final int SAME_FRAME_EXTENDED = 251; // fb
  static final int APPEND_FRAME = 252; // to 254 // fc-fe
  static final int FULL_FRAME = 255; // ff
  private static final int FRAMES = 0;
  private static final int MAXS = 1;
  private static final int NOTHING = 2;
  MethodWriter next;
  final ClassWriter cw;
  private int access;
  private final int name;
  private final int desc;
  private final String descriptor;
  String signature;
  int classReaderOffset;
  int classReaderLength;
  int exceptionCount;
  int[] exceptions;
  private ByteVector annd;
  private AnnotationWriter anns;
  private AnnotationWriter ianns;
  private AnnotationWriter[] panns;
  private AnnotationWriter[] ipanns;
  private int synthetics;
  private Attribute attrs;
  private ByteVector code = new ByteVector();
  private int maxStack;
  private int maxLocals;
  private int frameCount;
  private ByteVector stackMap;
  private int previousFrameOffset;
  private int[] previousFrame;
  private int frameIndex;
  private int[] frame;
  private int handlerCount;
  private Handler firstHandler;
  private Handler lastHandler;
  private int localVarCount;
  private ByteVector localVar;
  private int localVarTypeCount;
  private ByteVector localVarType;
  private int lineNumberCount;
  private ByteVector lineNumber;
  private Attribute cattrs;
  private boolean resize;
  private int subroutines;
  private final int compute;
  private Label labels;
  private Label previousBlock;
  private Label currentBlock;
  private int stackSize;
  private int maxStackSize;

  MethodWriter(final ClassWriter cw,final int access,final String name,final String desc,final String signature,final String[] exceptions,final boolean computeMaxs,final boolean computeFrames){
    if(cw.firstMethod==null){
      cw.firstMethod = this;
    }else{
      cw.lastMethod.next = this;
    }
    cw.lastMethod = this;
    this.cw = cw;
    this.access = access;
    this.name = cw.newUTF8(name);
    this.desc = cw.newUTF8(desc);
    this.descriptor = desc;
    if(ClassReader.SIGNATURES){
      this.signature = signature;
    }
    if(exceptions!=null&&exceptions.length>0){
      exceptionCount = exceptions.length;
      this.exceptions = new int[exceptionCount];
      for(int i = 0;i<exceptionCount;++i){
        this.exceptions[i] = cw.newClass(exceptions[i]);
      }
    }
    this.compute = computeFrames?FRAMES:(computeMaxs?MAXS:NOTHING);
    if(computeMaxs||computeFrames){
      if(computeFrames&&"<init>".equals(name)){
        this.access |= ACC_CONSTRUCTOR;
      }
      // updates maxLocals
      int size = Type.getArgumentsAndReturnSizes(descriptor)>>2;
      if((access&Opcodes.ACC_STATIC)!=0){
        --size;
      }
      maxLocals = size;
      // creates and visits the label for the first basic block
      labels = new Label();
      labels.status |= Label.PUSHED;
      visitLabel(labels);
    }
  }

  public AnnotationVisitor visitAnnotationDefault(){
    if(!ClassReader.ANNOTATIONS){
      return null;
    }
    annd = new ByteVector();
    return new AnnotationWriter(cw,false,annd,null,0);
  }

  public AnnotationVisitor visitAnnotation(final String desc,final boolean visible){
    if(!ClassReader.ANNOTATIONS){
      return null;
    }
    ByteVector bv = new ByteVector();
    // write type, and reserve space for values count
    bv.putShort(cw.newUTF8(desc)).putShort(0);
    AnnotationWriter aw = new AnnotationWriter(cw,true,bv,bv,2);
    if(visible){
      aw.next = anns;
      anns = aw;
    }else{
      aw.next = ianns;
      ianns = aw;
    }
    return aw;
  }

  public AnnotationVisitor visitParameterAnnotation(final int parameter,final String desc,final boolean visible){
    if(!ClassReader.ANNOTATIONS){
      return null;
    }
    ByteVector bv = new ByteVector();
    if("Ljava/lang/Synthetic;".equals(desc)){
      // workaround for a bug in javac with synthetic parameters
      // see ClassReader.readParameterAnnotations
      synthetics = Math.max(synthetics,parameter+1);
      return new AnnotationWriter(cw,false,bv,null,0);
    }
    // write type, and reserve space for values count
    bv.putShort(cw.newUTF8(desc)).putShort(0);
    AnnotationWriter aw = new AnnotationWriter(cw,true,bv,bv,2);
    if(visible){
      if(panns==null){
        panns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      aw.next = panns[parameter];
      panns[parameter] = aw;
    }else{
      if(ipanns==null){
        ipanns = new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      aw.next = ipanns[parameter];
      ipanns[parameter] = aw;
    }
    return aw;
  }

  public void visitAttribute(final Attribute attr){
    if(attr.isCodeAttribute()){
      attr.next = cattrs;
      cattrs = attr;
    }else{
      attr.next = attrs;
      attrs = attr;
    }
  }

  public void visitCode(){}

  public void visitFrame(final int type,final int nLocal,final Object[] local,final int nStack,final Object[] stack){
    if(!ClassReader.FRAMES||compute==FRAMES){
      return;
    }
    if(type==Opcodes.F_NEW){
      startFrame(code.length,nLocal,nStack);
      for(int i = 0;i<nLocal;++i){
        if(local[i] instanceof String){
          frame[frameIndex++] = Frame.OBJECT|cw.addType((String)local[i]);
        }else if(local[i] instanceof Integer){
          frame[frameIndex++] = ((Integer)local[i]).intValue();
        }else{
          frame[frameIndex++] = Frame.UNINITIALIZED|cw.addUninitializedType("",((Label)local[i]).position);
        }
      }
      for(int i = 0;i<nStack;++i){
        if(stack[i] instanceof String){
          frame[frameIndex++] = Frame.OBJECT|cw.addType((String)stack[i]);
        }else if(stack[i] instanceof Integer){
          frame[frameIndex++] = ((Integer)stack[i]).intValue();
        }else{
          frame[frameIndex++] = Frame.UNINITIALIZED|cw.addUninitializedType("",((Label)stack[i]).position);
        }
      }
      endFrame();
    }else{
      int delta;
      if(stackMap==null){
        stackMap = new ByteVector();
        delta = code.length;
      }else{
        delta = code.length-previousFrameOffset-1;
        if(delta<0){
          if(type==Opcodes.F_SAME){
            return;
          }else{
            throw new IllegalStateException();
          }
        }
      }
      switch(type){
        case Opcodes.F_FULL:
          stackMap.putByte(FULL_FRAME).putShort(delta).putShort(nLocal);
          for(int i = 0;i<nLocal;++i){
            writeFrameType(local[i]);
          }
          stackMap.putShort(nStack);
          for(int i = 0;i<nStack;++i){
            writeFrameType(stack[i]);
          }
          break;
        case Opcodes.F_APPEND:
          stackMap.putByte(SAME_FRAME_EXTENDED+nLocal).putShort(delta);
          for(int i = 0;i<nLocal;++i){
            writeFrameType(local[i]);
          }
          break;
        case Opcodes.F_CHOP:
          stackMap.putByte(SAME_FRAME_EXTENDED-nLocal).putShort(delta);
          break;
        case Opcodes.F_SAME:
          if(delta<64){
            stackMap.putByte(delta);
          }else{
            stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
          }
          break;
        case Opcodes.F_SAME1:
          if(delta<64){
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME+delta);
          }else{
            stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
          }
          writeFrameType(stack[0]);
          break;
      }
      previousFrameOffset = code.length;
      ++frameCount;
    }
  }

  public void visitInsn(final int opcode){
    // adds the instruction to the bytecode of the method
    code.putByte(opcode);
    // update currentBlock
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,0,null,null);
      }else{
        // updates current and max stack sizes
        int size = stackSize+Frame.SIZE[opcode];
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
      // if opcode == ATHROW or xRETURN, ends current block (no successor)
      if((opcode>=Opcodes.IRETURN&&opcode<=Opcodes.RETURN)||opcode==Opcodes.ATHROW){
        noSuccessor();
      }
    }
  }

  public void visitIntInsn(final int opcode,final int operand){
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,operand,null,null);
      }else if(opcode!=Opcodes.NEWARRAY){
        // updates current and max stack sizes only for NEWARRAY
        // (stack size variation = 0 for BIPUSH or SIPUSH)
        int size = stackSize+1;
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
    }
    // adds the instruction to the bytecode of the method
    if(opcode==Opcodes.SIPUSH){
      code.put12(opcode,operand);
    }else{ // BIPUSH or NEWARRAY
      code.put11(opcode,operand);
    }
  }

  public void visitVarInsn(final int opcode,final int var){
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,var,null,null);
      }else{
        // updates current and max stack sizes
        if(opcode==Opcodes.RET){
          // no stack change, but end of current block (no successor)
          currentBlock.status |= Label.RET;
          // save 'stackSize' here for future use
          // (see {@link #findSubroutineSuccessors})
          currentBlock.inputStackTop = stackSize;
          noSuccessor();
        }else{ // xLOAD or xSTORE
          int size = stackSize+Frame.SIZE[opcode];
          if(size>maxStackSize){
            maxStackSize = size;
          }
          stackSize = size;
        }
      }
    }
    if(compute!=NOTHING){
      // updates max locals
      int n;
      if(opcode==Opcodes.LLOAD||opcode==Opcodes.DLOAD||opcode==Opcodes.LSTORE||opcode==Opcodes.DSTORE){
        n = var+2;
      }else{
        n = var+1;
      }
      if(n>maxLocals){
        maxLocals = n;
      }
    }
    // adds the instruction to the bytecode of the method
    if(var<4&&opcode!=Opcodes.RET){
      int opt;
      if(opcode<Opcodes.ISTORE){
        /* ILOAD_0 */
        opt = 26+((opcode-Opcodes.ILOAD)<<2)+var;
      }else{
        /* ISTORE_0 */
        opt = 59+((opcode-Opcodes.ISTORE)<<2)+var;
      }
      code.putByte(opt);
    }else if(var>=256){
      code.putByte(196 /* WIDE */).put12(opcode,var);
    }else{
      code.put11(opcode,var);
    }
    if(opcode>=Opcodes.ISTORE&&compute==FRAMES&&handlerCount>0){
      visitLabel(new Label());
    }
  }

  public void visitTypeInsn(final int opcode,final String type){
    Item i = cw.newClassItem(type);
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,code.length,cw,i);
      }else if(opcode==Opcodes.NEW){
        // updates current and max stack sizes only if opcode == NEW
        // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF)
        int size = stackSize+1;
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
    }
    // adds the instruction to the bytecode of the method
    code.put12(opcode,i.index);
  }

  public void visitFieldInsn(final int opcode,final String owner,final String name,final String desc){
    Item i = cw.newFieldItem(owner,name,desc);
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,0,cw,i);
      }else{
        int size;
        // computes the stack size variation
        char c = desc.charAt(0);
        switch(opcode){
          case Opcodes.GETSTATIC:
            size = stackSize+(c=='D'||c=='J'?2:1);
            break;
          case Opcodes.PUTSTATIC:
            size = stackSize+(c=='D'||c=='J'?-2:-1);
            break;
          case Opcodes.GETFIELD:
            size = stackSize+(c=='D'||c=='J'?1:0);
            break;
          // case Constants.PUTFIELD:
          default:
            size = stackSize+(c=='D'||c=='J'?-3:-2);
            break;
        }
        // updates current and max stack sizes
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
    }
    // adds the instruction to the bytecode of the method
    code.put12(opcode,i.index);
  }

  public void visitMethodInsn(final int opcode,final String owner,final String name,final String desc){
    boolean itf = opcode==Opcodes.INVOKEINTERFACE;
    Item i = (opcode==Opcodes.INVOKEDYNAMIC)?cw.newNameTypeItem(name,desc):cw.newMethodItem(owner,name,desc,itf);
    int argSize = i.intVal;
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,0,cw,i);
      }else{
        if(argSize==0){
          // the above sizes have not been computed yet,
          // so we compute them...
          argSize = Type.getArgumentsAndReturnSizes(desc);
          // ... and we save them in order
          // not to recompute them in the future
          i.intVal = argSize;
        }
        int size;
        if(opcode==Opcodes.INVOKESTATIC||opcode==Opcodes.INVOKEDYNAMIC){
          size = stackSize-(argSize>>2)+(argSize&0x03)+1;
        }else{
          size = stackSize-(argSize>>2)+(argSize&0x03);
        }
        // updates current and max stack sizes
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
    }
    // adds the instruction to the bytecode of the method
    if(itf){
      if(argSize==0){
        argSize = Type.getArgumentsAndReturnSizes(desc);
        i.intVal = argSize;
      }
      code.put12(Opcodes.INVOKEINTERFACE,i.index).put11(argSize>>2,0);
    }else{
      code.put12(opcode,i.index);
      if(opcode==Opcodes.INVOKEDYNAMIC){
        code.putShort(0);
      }
    }
  }

  public void visitJumpInsn(final int opcode,final Label label){
    Label nextInsn = null;
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(opcode,0,null,null);
        // 'label' is the target of a jump instruction
        label.getFirst().status |= Label.TARGET;
        // adds 'label' as a successor of this basic block
        addSuccessor(Edge.NORMAL,label);
        if(opcode!=Opcodes.GOTO){
          // creates a Label for the next basic block
          nextInsn = new Label();
        }
      }else{
        if(opcode==Opcodes.JSR){
          if((label.status&Label.SUBROUTINE)==0){
            label.status |= Label.SUBROUTINE;
            ++subroutines;
          }
          currentBlock.status |= Label.JSR;
          addSuccessor(stackSize+1,label);
          // creates a Label for the next basic block
          nextInsn = new Label();
        }else{
          stackSize += Frame.SIZE[opcode];
          addSuccessor(stackSize,label);
        }
      }
    }
    // adds the instruction to the bytecode of the method
    if((label.status&Label.RESOLVED)!=0&&label.position-code.length<Short.MIN_VALUE){
      if(opcode==Opcodes.GOTO){
        code.putByte(200); // GOTO_W
      }else if(opcode==Opcodes.JSR){
        code.putByte(201); // JSR_W
      }else{
        // if the IF instruction is transformed into IFNOT GOTO_W the
        // next instruction becomes the target of the IFNOT instruction
        if(nextInsn!=null){
          nextInsn.status |= Label.TARGET;
        }
        code.putByte(opcode<=166?((opcode+1)^1)-1:opcode^1);
        code.putShort(8); // jump offset
        code.putByte(200); // GOTO_W
      }
      label.put(this,code,code.length-1,true);
    }else{
      /*
       * case of a backward jump with an offset >= -32768, or of a forward jump with, of course, an
       * unknown offset. In these cases we store the offset in 2 bytes (which will be increased in
       * resizeInstructions, if needed).
       */
      code.putByte(opcode);
      label.put(this,code,code.length-1,false);
    }
    if(currentBlock!=null){
      if(nextInsn!=null){
        // if the jump instruction is not a GOTO, the next instruction
        // is also a successor of this instruction. Calling visitLabel
        // adds the label of this next instruction as a successor of the
        // current block, and starts a new basic block
        visitLabel(nextInsn);
      }
      if(opcode==Opcodes.GOTO){
        noSuccessor();
      }
    }
  }

  public void visitLabel(final Label label){
    // resolves previous forward references to label, if any
    resize |= label.resolve(this,code.length,code.data);
    // updates currentBlock
    if((label.status&Label.DEBUG)!=0){
      return;
    }
    if(compute==FRAMES){
      if(currentBlock!=null){
        if(label.position==currentBlock.position){
          // successive labels, do not start a new basic block
          currentBlock.status |= (label.status&Label.TARGET);
          label.frame = currentBlock.frame;
          return;
        }
        // ends current block (with one new successor)
        addSuccessor(Edge.NORMAL,label);
      }
      // begins a new current block
      currentBlock = label;
      if(label.frame==null){
        label.frame = new Frame();
        label.frame.owner = label;
      }
      // updates the basic block list
      if(previousBlock!=null){
        if(label.position==previousBlock.position){
          previousBlock.status |= (label.status&Label.TARGET);
          label.frame = previousBlock.frame;
          currentBlock = previousBlock;
          return;
        }
        previousBlock.successor = label;
      }
      previousBlock = label;
    }else if(compute==MAXS){
      if(currentBlock!=null){
        // ends current block (with one new successor)
        currentBlock.outputStackMax = maxStackSize;
        addSuccessor(stackSize,label);
      }
      // begins a new current block
      currentBlock = label;
      // resets the relative current and max stack sizes
      stackSize = 0;
      maxStackSize = 0;
      // updates the basic block list
      if(previousBlock!=null){
        previousBlock.successor = label;
      }
      previousBlock = label;
    }
  }

  public void visitLdcInsn(final Object cst){
    Item i = cw.newConstItem(cst);
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(Opcodes.LDC,0,cw,i);
      }else{
        int size;
        // computes the stack size variation
        if(i.type==ClassWriter.LONG||i.type==ClassWriter.DOUBLE){
          size = stackSize+2;
        }else{
          size = stackSize+1;
        }
        // updates current and max stack sizes
        if(size>maxStackSize){
          maxStackSize = size;
        }
        stackSize = size;
      }
    }
    // adds the instruction to the bytecode of the method
    int index = i.index;
    if(i.type==ClassWriter.LONG||i.type==ClassWriter.DOUBLE){
      code.put12(20 /* LDC2_W */,index);
    }else if(index>=256){
      code.put12(19 /* LDC_W */,index);
    }else{
      code.put11(Opcodes.LDC,index);
    }
  }

  public void visitIincInsn(final int var,final int increment){
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(Opcodes.IINC,var,null,null);
      }
    }
    if(compute!=NOTHING){
      // updates max locals
      int n = var+1;
      if(n>maxLocals){
        maxLocals = n;
      }
    }
    // adds the instruction to the bytecode of the method
    if((var>255)||(increment>127)||(increment<-128)){
      code.putByte(196 /* WIDE */).put12(Opcodes.IINC,var).putShort(increment);
    }else{
      code.putByte(Opcodes.IINC).put11(var,increment);
    }
  }

  public void visitTableSwitchInsn(final int min,final int max,final Label dflt,final Label[] labels){
    // adds the instruction to the bytecode of the method
    int source = code.length;
    code.putByte(Opcodes.TABLESWITCH);
    code.putByteArray(null,0,(4-code.length%4)%4);
    dflt.put(this,code,source,true);
    code.putInt(min).putInt(max);
    for(int i = 0;i<labels.length;++i){
      labels[i].put(this,code,source,true);
    }
    // updates currentBlock
    visitSwitchInsn(dflt,labels);
  }

  public void visitLookupSwitchInsn(final Label dflt,final int[] keys,final Label[] labels){
    // adds the instruction to the bytecode of the method
    int source = code.length;
    code.putByte(Opcodes.LOOKUPSWITCH);
    code.putByteArray(null,0,(4-code.length%4)%4);
    dflt.put(this,code,source,true);
    code.putInt(labels.length);
    for(int i = 0;i<labels.length;++i){
      code.putInt(keys[i]);
      labels[i].put(this,code,source,true);
    }
    // updates currentBlock
    visitSwitchInsn(dflt,labels);
  }

  private void visitSwitchInsn(final Label dflt,final Label[] labels){
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(Opcodes.LOOKUPSWITCH,0,null,null);
        // adds current block successors
        addSuccessor(Edge.NORMAL,dflt);
        dflt.getFirst().status |= Label.TARGET;
        for(int i = 0;i<labels.length;++i){
          addSuccessor(Edge.NORMAL,labels[i]);
          labels[i].getFirst().status |= Label.TARGET;
        }
      }else{
        // updates current stack size (max stack size unchanged)
        --stackSize;
        // adds current block successors
        addSuccessor(stackSize,dflt);
        for(int i = 0;i<labels.length;++i){
          addSuccessor(stackSize,labels[i]);
        }
      }
      // ends current block
      noSuccessor();
    }
  }

  public void visitMultiANewArrayInsn(final String desc,final int dims){
    Item i = cw.newClassItem(desc);
    // Label currentBlock = this.currentBlock;
    if(currentBlock!=null){
      if(compute==FRAMES){
        currentBlock.frame.execute(Opcodes.MULTIANEWARRAY,dims,cw,i);
      }else{
        // updates current stack size (max stack size unchanged because
        // stack size variation always negative or null)
        stackSize += 1-dims;
      }
    }
    // adds the instruction to the bytecode of the method
    code.put12(Opcodes.MULTIANEWARRAY,i.index).putByte(dims);
  }

  public void visitTryCatchBlock(final Label start,final Label end,final Label handler,final String type){
    ++handlerCount;
    Handler h = new Handler();
    h.start = start;
    h.end = end;
    h.handler = handler;
    h.desc = type;
    h.type = type!=null?cw.newClass(type):0;
    if(lastHandler==null){
      firstHandler = h;
    }else{
      lastHandler.next = h;
    }
    lastHandler = h;
  }

  public void visitLocalVariable(final String name,final String desc,final String signature,final Label start,final Label end,final int index){
    if(signature!=null){
      if(localVarType==null){
        localVarType = new ByteVector();
      }
      ++localVarTypeCount;
      localVarType.putShort(start.position).putShort(end.position-start.position).putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature)).putShort(index);
    }
    if(localVar==null){
      localVar = new ByteVector();
    }
    ++localVarCount;
    localVar.putShort(start.position).putShort(end.position-start.position).putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc)).putShort(index);
    if(compute!=NOTHING){
      // updates max locals
      char c = desc.charAt(0);
      int n = index+(c=='J'||c=='D'?2:1);
      if(n>maxLocals){
        maxLocals = n;
      }
    }
  }

  public void visitLineNumber(final int line,final Label start){
    if(lineNumber==null){
      lineNumber = new ByteVector();
    }
    ++lineNumberCount;
    lineNumber.putShort(start.position);
    lineNumber.putShort(line);
  }

  public void visitMaxs(final int maxStack,final int maxLocals){
    if(ClassReader.FRAMES&&compute==FRAMES){
      // completes the control flow graph with exception handler blocks
      Handler handler = firstHandler;
      while(handler!=null){
        Label l = handler.start.getFirst();
        Label h = handler.handler.getFirst();
        Label e = handler.end.getFirst();
        // computes the kind of the edges to 'h'
        String t = handler.desc==null?"java/lang/Throwable":handler.desc;
        int kind = Frame.OBJECT|cw.addType(t);
        // h is an exception handler
        h.status |= Label.TARGET;
        // adds 'h' as a successor of labels between 'start' and 'end'
        while(l!=e){
          // creates an edge to 'h'
          Edge b = new Edge();
          b.info = kind;
          b.successor = h;
          // adds it to the successors of 'l'
          b.next = l.successors;
          l.successors = b;
          // goes to the next label
          l = l.successor;
        }
        handler = handler.next;
      }
      // creates and visits the first (implicit) frame
      Frame f = labels.frame;
      Type[] args = Type.getArgumentTypes(descriptor);
      f.initInputFrame(cw,access,args,this.maxLocals);
      visitFrame(f);
      int max = 0;
      Label changed = labels;
      while(changed!=null){
        // removes a basic block from the list of changed basic blocks
        Label l = changed;
        changed = changed.next;
        l.next = null;
        f = l.frame;
        // a reachable jump target must be stored in the stack map
        if((l.status&Label.TARGET)!=0){
          l.status |= Label.STORE;
        }
        // all visited labels are reachable, by definition
        l.status |= Label.REACHABLE;
        // updates the (absolute) maximum stack size
        int blockMax = f.inputStack.length+l.outputStackMax;
        if(blockMax>max){
          max = blockMax;
        }
        // updates the successors of the current basic block
        Edge e = l.successors;
        while(e!=null){
          Label n = e.successor.getFirst();
          boolean change = f.merge(cw,n.frame,e.info);
          if(change&&n.next==null){
            // if n has changed and is not already in the 'changed'
            // list, adds it to this list
            n.next = changed;
            changed = n;
          }
          e = e.next;
        }
      }
      // visits all the frames that must be stored in the stack map
      Label l = labels;
      while(l!=null){
        f = l.frame;
        if((l.status&Label.STORE)!=0){
          visitFrame(f);
        }
        if((l.status&Label.REACHABLE)==0){
          // finds start and end of dead basic block
          Label k = l.successor;
          int start = l.position;
          int end = (k==null?code.length:k.position)-1;
          // if non empty basic block
          if(end>=start){
            max = Math.max(max,1);
            // replaces instructions with NOP ... NOP ATHROW
            for(int i = start;i<end;++i){
              code.data[i] = Opcodes.NOP;
            }
            code.data[end] = (byte)Opcodes.ATHROW;
            // emits a frame for this unreachable block
            startFrame(start,0,1);
            frame[frameIndex++] = Frame.OBJECT|cw.addType("java/lang/Throwable");
            endFrame();
          }
        }
        l = l.successor;
      }
      this.maxStack = max;
    }else if(compute==MAXS){
      // completes the control flow graph with exception handler blocks
      Handler handler = firstHandler;
      while(handler!=null){
        Label l = handler.start;
        Label h = handler.handler;
        Label e = handler.end;
        // adds 'h' as a successor of labels between 'start' and 'end'
        while(l!=e){
          // creates an edge to 'h'
          Edge b = new Edge();
          b.info = Edge.EXCEPTION;
          b.successor = h;
          // adds it to the successors of 'l'
          if((l.status&Label.JSR)==0){
            b.next = l.successors;
            l.successors = b;
          }else{
            // if l is a JSR block, adds b after the first two edges
            // to preserve the hypothesis about JSR block successors
            // order (see {@link #visitJumpInsn})
            b.next = l.successors.next.next;
            l.successors.next.next = b;
          }
          // goes to the next label
          l = l.successor;
        }
        handler = handler.next;
      }
      if(subroutines>0){
        int id = 0;
        labels.visitSubroutine(null,1,subroutines);
        // finds the basic blocks that belong to the real subroutines
        Label l = labels;
        while(l!=null){
          if((l.status&Label.JSR)!=0){
            // the subroutine is defined by l's TARGET, not by l
            Label subroutine = l.successors.next.successor;
            // if this subroutine has not been visited yet...
            if((subroutine.status&Label.VISITED)==0){
              // ...assigns it a new id and finds its basic blocks
              id += 1;
              subroutine.visitSubroutine(null,(id/32L)<<32|(1L<<(id%32)),subroutines);
            }
          }
          l = l.successor;
        }
        // second step: finds the successors of RET blocks
        l = labels;
        while(l!=null){
          if((l.status&Label.JSR)!=0){
            Label L = labels;
            while(L!=null){
              L.status &= ~Label.VISITED2;
              L = L.successor;
            }
            // the subroutine is defined by l's TARGET, not by l
            Label subroutine = l.successors.next.successor;
            subroutine.visitSubroutine(l,0,subroutines);
          }
          l = l.successor;
        }
      }
      int max = 0;
      Label stack = labels;
      while(stack!=null){
        // pops a block from the stack
        Label l = stack;
        stack = stack.next;
        // computes the true (non relative) max stack size of this block
        int start = l.inputStackTop;
        int blockMax = start+l.outputStackMax;
        // updates the global max stack size
        if(blockMax>max){
          max = blockMax;
        }
        // analyzes the successors of the block
        Edge b = l.successors;
        if((l.status&Label.JSR)!=0){
          // ignores the first edge of JSR blocks (virtual successor)
          b = b.next;
        }
        while(b!=null){
          l = b.successor;
          // if this successor has not already been pushed...
          if((l.status&Label.PUSHED)==0){
            // computes its true beginning stack size...
            l.inputStackTop = b.info==Edge.EXCEPTION?1:start+b.info;
            // ...and pushes it onto the stack
            l.status |= Label.PUSHED;
            l.next = stack;
            stack = l;
          }
          b = b.next;
        }
      }
      this.maxStack = max;
    }else{
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }

  public void visitEnd(){}

  private void addSuccessor(final int info,final Label successor){
    // creates and initializes an Edge object...
    Edge b = new Edge();
    b.info = info;
    b.successor = successor;
    // ...and adds it to the successor list of the currentBlock block
    b.next = currentBlock.successors;
    currentBlock.successors = b;
  }

  private void noSuccessor(){
    if(compute==FRAMES){
      Label l = new Label();
      l.frame = new Frame();
      l.frame.owner = l;
      l.resolve(this,code.length,code.data);
      previousBlock.successor = l;
      previousBlock = l;
    }else{
      currentBlock.outputStackMax = maxStackSize;
    }
    currentBlock = null;
  }

  private void visitFrame(final Frame f){
    int i,t;
    int nTop = 0;
    int nLocal = 0;
    int nStack = 0;
    int[] locals = f.inputLocals;
    int[] stacks = f.inputStack;
    // computes the number of locals (ignores TOP types that are just after
    // a LONG or a DOUBLE, and all trailing TOP types)
    for(i = 0;i<locals.length;++i){
      t = locals[i];
      if(t==Frame.TOP){
        ++nTop;
      }else{
        nLocal += nTop+1;
        nTop = 0;
      }
      if(t==Frame.LONG||t==Frame.DOUBLE){
        ++i;
      }
    }
    // computes the stack size (ignores TOP types that are just after
    // a LONG or a DOUBLE)
    for(i = 0;i<stacks.length;++i){
      t = stacks[i];
      ++nStack;
      if(t==Frame.LONG||t==Frame.DOUBLE){
        ++i;
      }
    }
    // visits the frame and its content
    startFrame(f.owner.position,nLocal,nStack);
    for(i = 0;nLocal>0;++i,--nLocal){
      t = locals[i];
      frame[frameIndex++] = t;
      if(t==Frame.LONG||t==Frame.DOUBLE){
        ++i;
      }
    }
    for(i = 0;i<stacks.length;++i){
      t = stacks[i];
      frame[frameIndex++] = t;
      if(t==Frame.LONG||t==Frame.DOUBLE){
        ++i;
      }
    }
    endFrame();
  }

  private void startFrame(final int offset,final int nLocal,final int nStack){
    int n = 3+nLocal+nStack;
    if(frame==null||frame.length<n){
      frame = new int[n];
    }
    frame[0] = offset;
    frame[1] = nLocal;
    frame[2] = nStack;
    frameIndex = 3;
  }

  private void endFrame(){
    if(previousFrame!=null){ // do not write the first frame
      if(stackMap==null){
        stackMap = new ByteVector();
      }
      writeFrame();
      ++frameCount;
    }
    previousFrame = frame;
    frame = null;
  }

  private void writeFrame(){
    int clocalsSize = frame[1];
    int cstackSize = frame[2];
    if((cw.version&0xFFFF)<Opcodes.V1_6){
      stackMap.putShort(frame[0]).putShort(clocalsSize);
      writeFrameTypes(3,3+clocalsSize);
      stackMap.putShort(cstackSize);
      writeFrameTypes(3+clocalsSize,3+clocalsSize+cstackSize);
      return;
    }
    int localsSize = previousFrame[1];
    int type = FULL_FRAME;
    int k = 0;
    int delta;
    if(frameCount==0){
      delta = frame[0];
    }else{
      delta = frame[0]-previousFrame[0]-1;
    }
    if(cstackSize==0){
      k = clocalsSize-localsSize;
      switch(k){
        case -3:
        case -2:
        case -1:
          type = CHOP_FRAME;
          localsSize = clocalsSize;
          break;
        case 0:
          type = delta<64?SAME_FRAME:SAME_FRAME_EXTENDED;
          break;
        case 1:
        case 2:
        case 3:
          type = APPEND_FRAME;
          break;
      }
    }else if(clocalsSize==localsSize&&cstackSize==1){
      type = delta<63?SAME_LOCALS_1_STACK_ITEM_FRAME:SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
    }
    if(type!=FULL_FRAME){
      // verify if locals are the same
      int l = 3;
      for(int j = 0;j<localsSize;j++){
        if(frame[l]!=previousFrame[l]){
          type = FULL_FRAME;
          break;
        }
        l++;
      }
    }
    switch(type){
      case SAME_FRAME:
        stackMap.putByte(delta);
        break;
      case SAME_LOCALS_1_STACK_ITEM_FRAME:
        stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME+delta);
        writeFrameTypes(3+clocalsSize,4+clocalsSize);
        break;
      case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
        stackMap.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(delta);
        writeFrameTypes(3+clocalsSize,4+clocalsSize);
        break;
      case SAME_FRAME_EXTENDED:
        stackMap.putByte(SAME_FRAME_EXTENDED).putShort(delta);
        break;
      case CHOP_FRAME:
        stackMap.putByte(SAME_FRAME_EXTENDED+k).putShort(delta);
        break;
      case APPEND_FRAME:
        stackMap.putByte(SAME_FRAME_EXTENDED+k).putShort(delta);
        writeFrameTypes(3+localsSize,3+clocalsSize);
        break;
      // case FULL_FRAME:
      default:
        stackMap.putByte(FULL_FRAME).putShort(delta).putShort(clocalsSize);
        writeFrameTypes(3,3+clocalsSize);
        stackMap.putShort(cstackSize);
        writeFrameTypes(3+clocalsSize,3+clocalsSize+cstackSize);
    }
  }

  private void writeFrameTypes(final int start,final int end){
    for(int i = start;i<end;++i){
      int t = frame[i];
      int d = t&Frame.DIM;
      if(d==0){
        int v = t&Frame.BASE_VALUE;
        switch(t&Frame.BASE_KIND){
          case Frame.OBJECT:
            stackMap.putByte(7).putShort(cw.newClass(cw.typeTable[v].strVal1));
            break;
          case Frame.UNINITIALIZED:
            stackMap.putByte(8).putShort(cw.typeTable[v].intVal);
            break;
          default:
            stackMap.putByte(v);
        }
      }else{
        StringBuffer buf = new StringBuffer();
        d >>= 28;
        while(d-->0){
          buf.append('[');
        }
        if((t&Frame.BASE_KIND)==Frame.OBJECT){
          buf.append('L');
          buf.append(cw.typeTable[t&Frame.BASE_VALUE].strVal1);
          buf.append(';');
        }else{
          switch(t&0xF){
            case 1:
              buf.append('I');
              break;
            case 2:
              buf.append('F');
              break;
            case 3:
              buf.append('D');
              break;
            case 9:
              buf.append('Z');
              break;
            case 10:
              buf.append('B');
              break;
            case 11:
              buf.append('C');
              break;
            case 12:
              buf.append('S');
              break;
            default:
              buf.append('J');
          }
        }
        stackMap.putByte(7).putShort(cw.newClass(buf.toString()));
      }
    }
  }

  private void writeFrameType(final Object type){
    if(type instanceof String){
      stackMap.putByte(7).putShort(cw.newClass((String)type));
    }else if(type instanceof Integer){
      stackMap.putByte(((Integer)type).intValue());
    }else{
      stackMap.putByte(8).putShort(((Label)type).position);
    }
  }

  final int getSize(){
    if(classReaderOffset!=0){
      return 6+classReaderLength;
    }
    if(resize){
      // replaces the temporary jump opcodes introduced by Label.resolve.
      if(ClassReader.RESIZE){
        resizeInstructions();
      }else{
        throw new RuntimeException("Method code too large!");
      }
    }
    int size = 8;
    if(code.length>0){
      cw.newUTF8("Code");
      size += 18+code.length+8*handlerCount;
      if(localVar!=null){
        cw.newUTF8("LocalVariableTable");
        size += 8+localVar.length;
      }
      if(localVarType!=null){
        cw.newUTF8("LocalVariableTypeTable");
        size += 8+localVarType.length;
      }
      if(lineNumber!=null){
        cw.newUTF8("LineNumberTable");
        size += 8+lineNumber.length;
      }
      if(stackMap!=null){
        boolean zip = (cw.version&0xFFFF)>=Opcodes.V1_6;
        cw.newUTF8(zip?"StackMapTable":"StackMap");
        size += 8+stackMap.length;
      }
      if(cattrs!=null){
        size += cattrs.getSize(cw,code.data,code.length,maxStack,maxLocals);
      }
    }
    if(exceptionCount>0){
      cw.newUTF8("Exceptions");
      size += 8+2*exceptionCount;
    }
    if((access&Opcodes.ACC_SYNTHETIC)!=0&&((cw.version&0xFFFF)<Opcodes.V1_5||(access&ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)!=0)){
      cw.newUTF8("Synthetic");
      size += 6;
    }
    if((access&Opcodes.ACC_DEPRECATED)!=0){
      cw.newUTF8("Deprecated");
      size += 6;
    }
    if(ClassReader.SIGNATURES&&signature!=null){
      cw.newUTF8("Signature");
      cw.newUTF8(signature);
      size += 8;
    }
    if(ClassReader.ANNOTATIONS&&annd!=null){
      cw.newUTF8("AnnotationDefault");
      size += 6+annd.length;
    }
    if(ClassReader.ANNOTATIONS&&anns!=null){
      cw.newUTF8("RuntimeVisibleAnnotations");
      size += 8+anns.getSize();
    }
    if(ClassReader.ANNOTATIONS&&ianns!=null){
      cw.newUTF8("RuntimeInvisibleAnnotations");
      size += 8+ianns.getSize();
    }
    if(ClassReader.ANNOTATIONS&&panns!=null){
      cw.newUTF8("RuntimeVisibleParameterAnnotations");
      size += 7+2*(panns.length-synthetics);
      for(int i = panns.length-1;i>=synthetics;--i){
        size += panns[i]==null?0:panns[i].getSize();
      }
    }
    if(ClassReader.ANNOTATIONS&&ipanns!=null){
      cw.newUTF8("RuntimeInvisibleParameterAnnotations");
      size += 7+2*(ipanns.length-synthetics);
      for(int i = ipanns.length-1;i>=synthetics;--i){
        size += ipanns[i]==null?0:ipanns[i].getSize();
      }
    }
    if(attrs!=null){
      size += attrs.getSize(cw,null,0,-1,-1);
    }
    return size;
  }

  final void put(final ByteVector out){
    int mask = Opcodes.ACC_DEPRECATED|ClassWriter.ACC_SYNTHETIC_ATTRIBUTE|((access&ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)/(ClassWriter.ACC_SYNTHETIC_ATTRIBUTE/Opcodes.ACC_SYNTHETIC));
    out.putShort(access&~mask).putShort(name).putShort(desc);
    if(classReaderOffset!=0){
      out.putByteArray(cw.cr.b,classReaderOffset,classReaderLength);
      return;
    }
    int attributeCount = 0;
    if(code.length>0){
      ++attributeCount;
    }
    if(exceptionCount>0){
      ++attributeCount;
    }
    if((access&Opcodes.ACC_SYNTHETIC)!=0&&((cw.version&0xFFFF)<Opcodes.V1_5||(access&ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)!=0)){
      ++attributeCount;
    }
    if((access&Opcodes.ACC_DEPRECATED)!=0){
      ++attributeCount;
    }
    if(ClassReader.SIGNATURES&&signature!=null){
      ++attributeCount;
    }
    if(ClassReader.ANNOTATIONS&&annd!=null){
      ++attributeCount;
    }
    if(ClassReader.ANNOTATIONS&&anns!=null){
      ++attributeCount;
    }
    if(ClassReader.ANNOTATIONS&&ianns!=null){
      ++attributeCount;
    }
    if(ClassReader.ANNOTATIONS&&panns!=null){
      ++attributeCount;
    }
    if(ClassReader.ANNOTATIONS&&ipanns!=null){
      ++attributeCount;
    }
    if(attrs!=null){
      attributeCount += attrs.getCount();
    }
    out.putShort(attributeCount);
    if(code.length>0){
      int size = 12+code.length+8*handlerCount;
      if(localVar!=null){
        size += 8+localVar.length;
      }
      if(localVarType!=null){
        size += 8+localVarType.length;
      }
      if(lineNumber!=null){
        size += 8+lineNumber.length;
      }
      if(stackMap!=null){
        size += 8+stackMap.length;
      }
      if(cattrs!=null){
        size += cattrs.getSize(cw,code.data,code.length,maxStack,maxLocals);
      }
      out.putShort(cw.newUTF8("Code")).putInt(size);
      out.putShort(maxStack).putShort(maxLocals);
      out.putInt(code.length).putByteArray(code.data,0,code.length);
      out.putShort(handlerCount);
      if(handlerCount>0){
        Handler h = firstHandler;
        while(h!=null){
          out.putShort(h.start.position).putShort(h.end.position).putShort(h.handler.position).putShort(h.type);
          h = h.next;
        }
      }
      attributeCount = 0;
      if(localVar!=null){
        ++attributeCount;
      }
      if(localVarType!=null){
        ++attributeCount;
      }
      if(lineNumber!=null){
        ++attributeCount;
      }
      if(stackMap!=null){
        ++attributeCount;
      }
      if(cattrs!=null){
        attributeCount += cattrs.getCount();
      }
      out.putShort(attributeCount);
      if(localVar!=null){
        out.putShort(cw.newUTF8("LocalVariableTable"));
        out.putInt(localVar.length+2).putShort(localVarCount);
        out.putByteArray(localVar.data,0,localVar.length);
      }
      if(localVarType!=null){
        out.putShort(cw.newUTF8("LocalVariableTypeTable"));
        out.putInt(localVarType.length+2).putShort(localVarTypeCount);
        out.putByteArray(localVarType.data,0,localVarType.length);
      }
      if(lineNumber!=null){
        out.putShort(cw.newUTF8("LineNumberTable"));
        out.putInt(lineNumber.length+2).putShort(lineNumberCount);
        out.putByteArray(lineNumber.data,0,lineNumber.length);
      }
      if(stackMap!=null){
        boolean zip = (cw.version&0xFFFF)>=Opcodes.V1_6;
        out.putShort(cw.newUTF8(zip?"StackMapTable":"StackMap"));
        out.putInt(stackMap.length+2).putShort(frameCount);
        out.putByteArray(stackMap.data,0,stackMap.length);
      }
      if(cattrs!=null){
        cattrs.put(cw,code.data,code.length,maxLocals,maxStack,out);
      }
    }
    if(exceptionCount>0){
      out.putShort(cw.newUTF8("Exceptions")).putInt(2*exceptionCount+2);
      out.putShort(exceptionCount);
      for(int i = 0;i<exceptionCount;++i){
        out.putShort(exceptions[i]);
      }
    }
    if((access&Opcodes.ACC_SYNTHETIC)!=0&&((cw.version&0xFFFF)<Opcodes.V1_5||(access&ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)!=0)){
      out.putShort(cw.newUTF8("Synthetic")).putInt(0);
    }
    if((access&Opcodes.ACC_DEPRECATED)!=0){
      out.putShort(cw.newUTF8("Deprecated")).putInt(0);
    }
    if(ClassReader.SIGNATURES&&signature!=null){
      out.putShort(cw.newUTF8("Signature")).putInt(2).putShort(cw.newUTF8(signature));
    }
    if(ClassReader.ANNOTATIONS&&annd!=null){
      out.putShort(cw.newUTF8("AnnotationDefault"));
      out.putInt(annd.length);
      out.putByteArray(annd.data,0,annd.length);
    }
    if(ClassReader.ANNOTATIONS&&anns!=null){
      out.putShort(cw.newUTF8("RuntimeVisibleAnnotations"));
      anns.put(out);
    }
    if(ClassReader.ANNOTATIONS&&ianns!=null){
      out.putShort(cw.newUTF8("RuntimeInvisibleAnnotations"));
      ianns.put(out);
    }
    if(ClassReader.ANNOTATIONS&&panns!=null){
      out.putShort(cw.newUTF8("RuntimeVisibleParameterAnnotations"));
      AnnotationWriter.put(panns,synthetics,out);
    }
    if(ClassReader.ANNOTATIONS&&ipanns!=null){
      out.putShort(cw.newUTF8("RuntimeInvisibleParameterAnnotations"));
      AnnotationWriter.put(ipanns,synthetics,out);
    }
    if(attrs!=null){
      attrs.put(cw,null,0,-1,-1,out);
    }
  }

  private void resizeInstructions(){
    byte[] b = code.data; // bytecode of the method
    int u,v,label; // indexes in b
    int i,j; // loop indexes
    int[] allIndexes = new int[0]; // copy of indexes
    int[] allSizes = new int[0]; // copy of sizes
    boolean[] resize; // instructions to be resized
    int newOffset; // future offset of a jump instruction
    resize = new boolean[code.length];
    // 3 = loop again, 2 = loop ended, 1 = last pass, 0 = done
    int state = 3;
    do{
      if(state==3){
        state = 2;
      }
      u = 0;
      while(u<b.length){
        int opcode = b[u]&0xFF; // opcode of current instruction
        int insert = 0; // bytes to be added after this instruction
        switch(ClassWriter.TYPE[opcode]){
          case ClassWriter.NOARG_INSN:
          case ClassWriter.IMPLVAR_INSN:
            u += 1;
            break;
          case ClassWriter.LABEL_INSN:
            if(opcode>201){
              // converts temporary opcodes 202 to 217, 218 and
              // 219 to IFEQ ... JSR (inclusive), IFNULL and
              // IFNONNULL
              opcode = opcode<218?opcode-49:opcode-20;
              label = u+readUnsignedShort(b,u+1);
            }else{
              label = u+readShort(b,u+1);
            }
            newOffset = getNewOffset(allIndexes,allSizes,u,label);
            if(newOffset<Short.MIN_VALUE||newOffset>Short.MAX_VALUE){
              if(!resize[u]){
                if(opcode==Opcodes.GOTO||opcode==Opcodes.JSR){
                  insert = 2;
                }else{
                  insert = 5;
                }
                resize[u] = true;
              }
            }
            u += 3;
            break;
          case ClassWriter.LABELW_INSN:
            u += 5;
            break;
          case ClassWriter.TABL_INSN:
            if(state==1){
              newOffset = getNewOffset(allIndexes,allSizes,0,u);
              insert = -(newOffset&3);
            }else if(!resize[u]){
              insert = u&3;
              resize[u] = true;
            }
            // skips instruction
            u = u+4-(u&3);
            u += 4*(readInt(b,u+8)-readInt(b,u+4)+1)+12;
            break;
          case ClassWriter.LOOK_INSN:
            if(state==1){
              // like TABL_INSN
              newOffset = getNewOffset(allIndexes,allSizes,0,u);
              insert = -(newOffset&3);
            }else if(!resize[u]){
              // like TABL_INSN
              insert = u&3;
              resize[u] = true;
            }
            // skips instruction
            u = u+4-(u&3);
            u += 8*readInt(b,u+4)+8;
            break;
          case ClassWriter.WIDE_INSN:
            opcode = b[u+1]&0xFF;
            if(opcode==Opcodes.IINC){
              u += 6;
            }else{
              u += 4;
            }
            break;
          case ClassWriter.VAR_INSN:
          case ClassWriter.SBYTE_INSN:
          case ClassWriter.LDC_INSN:
            u += 2;
            break;
          case ClassWriter.SHORT_INSN:
          case ClassWriter.LDCW_INSN:
          case ClassWriter.FIELDORMETH_INSN:
          case ClassWriter.TYPE_INSN:
          case ClassWriter.IINC_INSN:
            u += 3;
            break;
          case ClassWriter.ITFDYNMETH_INSN:
            u += 5;
            break;
          // case ClassWriter.MANA_INSN:
          default:
            u += 4;
            break;
        }
        if(insert!=0){
          // adds a new (u, insert) entry in the allIndexes and
          // allSizes arrays
          int[] newIndexes = new int[allIndexes.length+1];
          int[] newSizes = new int[allSizes.length+1];
          System.arraycopy(allIndexes,0,newIndexes,0,allIndexes.length);
          System.arraycopy(allSizes,0,newSizes,0,allSizes.length);
          newIndexes[allIndexes.length] = u;
          newSizes[allSizes.length] = insert;
          allIndexes = newIndexes;
          allSizes = newSizes;
          if(insert>0){
            state = 3;
          }
        }
      }
      if(state<3){
        --state;
      }
    }while(state!=0);
    ByteVector newCode = new ByteVector(code.length);
    u = 0;
    while(u<code.length){
      int opcode = b[u]&0xFF;
      switch(ClassWriter.TYPE[opcode]){
        case ClassWriter.NOARG_INSN:
        case ClassWriter.IMPLVAR_INSN:
          newCode.putByte(opcode);
          u += 1;
          break;
        case ClassWriter.LABEL_INSN:
          if(opcode>201){
            opcode = opcode<218?opcode-49:opcode-20;
            label = u+readUnsignedShort(b,u+1);
          }else{
            label = u+readShort(b,u+1);
          }
          newOffset = getNewOffset(allIndexes,allSizes,u,label);
          if(resize[u]){
            if(opcode==Opcodes.GOTO){
              newCode.putByte(200); // GOTO_W
            }else if(opcode==Opcodes.JSR){
              newCode.putByte(201); // JSR_W
            }else{
              newCode.putByte(opcode<=166?((opcode+1)^1)-1:opcode^1);
              newCode.putShort(8); // jump offset
              newCode.putByte(200); // GOTO_W
              // newOffset now computed from start of GOTO_W
              newOffset -= 3;
            }
            newCode.putInt(newOffset);
          }else{
            newCode.putByte(opcode);
            newCode.putShort(newOffset);
          }
          u += 3;
          break;
        case ClassWriter.LABELW_INSN:
          label = u+readInt(b,u+1);
          newOffset = getNewOffset(allIndexes,allSizes,u,label);
          newCode.putByte(opcode);
          newCode.putInt(newOffset);
          u += 5;
          break;
        case ClassWriter.TABL_INSN:
          // skips 0 to 3 padding bytes
          v = u;
          u = u+4-(v&3);
          // reads and copies instruction
          newCode.putByte(Opcodes.TABLESWITCH);
          newCode.putByteArray(null,0,(4-newCode.length%4)%4);
          label = v+readInt(b,u);
          u += 4;
          newOffset = getNewOffset(allIndexes,allSizes,v,label);
          newCode.putInt(newOffset);
          j = readInt(b,u);
          u += 4;
          newCode.putInt(j);
          j = readInt(b,u)-j+1;
          u += 4;
          newCode.putInt(readInt(b,u-4));
          for(;j>0;--j){
            label = v+readInt(b,u);
            u += 4;
            newOffset = getNewOffset(allIndexes,allSizes,v,label);
            newCode.putInt(newOffset);
          }
          break;
        case ClassWriter.LOOK_INSN:
          // skips 0 to 3 padding bytes
          v = u;
          u = u+4-(v&3);
          // reads and copies instruction
          newCode.putByte(Opcodes.LOOKUPSWITCH);
          newCode.putByteArray(null,0,(4-newCode.length%4)%4);
          label = v+readInt(b,u);
          u += 4;
          newOffset = getNewOffset(allIndexes,allSizes,v,label);
          newCode.putInt(newOffset);
          j = readInt(b,u);
          u += 4;
          newCode.putInt(j);
          for(;j>0;--j){
            newCode.putInt(readInt(b,u));
            u += 4;
            label = v+readInt(b,u);
            u += 4;
            newOffset = getNewOffset(allIndexes,allSizes,v,label);
            newCode.putInt(newOffset);
          }
          break;
        case ClassWriter.WIDE_INSN:
          opcode = b[u+1]&0xFF;
          if(opcode==Opcodes.IINC){
            newCode.putByteArray(b,u,6);
            u += 6;
          }else{
            newCode.putByteArray(b,u,4);
            u += 4;
          }
          break;
        case ClassWriter.VAR_INSN:
        case ClassWriter.SBYTE_INSN:
        case ClassWriter.LDC_INSN:
          newCode.putByteArray(b,u,2);
          u += 2;
          break;
        case ClassWriter.SHORT_INSN:
        case ClassWriter.LDCW_INSN:
        case ClassWriter.FIELDORMETH_INSN:
        case ClassWriter.TYPE_INSN:
        case ClassWriter.IINC_INSN:
          newCode.putByteArray(b,u,3);
          u += 3;
          break;
        case ClassWriter.ITFDYNMETH_INSN:
          newCode.putByteArray(b,u,5);
          u += 5;
          break;
        // case MANA_INSN:
        default:
          newCode.putByteArray(b,u,4);
          u += 4;
          break;
      }
    }
    // recomputes the stack map frames
    if(frameCount>0){
      if(compute==FRAMES){
        frameCount = 0;
        stackMap = null;
        previousFrame = null;
        frame = null;
        Frame f = new Frame();
        f.owner = labels;
        Type[] args = Type.getArgumentTypes(descriptor);
        f.initInputFrame(cw,access,args,maxLocals);
        visitFrame(f);
        Label l = labels;
        while(l!=null){
          /*
           * here we need the original label position. getNewOffset must therefore never have been
           * called for this label.
           */
          u = l.position-3;
          if((l.status&Label.STORE)!=0||(u>=0&&resize[u])){
            getNewOffset(allIndexes,allSizes,l);
            // TODO update offsets in UNINITIALIZED values
            visitFrame(l.frame);
          }
          l = l.successor;
        }
      }else{
        /*
         * Resizing an existing stack map frame table is really hard. Not only the table must be
         * parsed to update the offets, but new frames may be needed for jump instructions that were
         * inserted by this method. And updating the offsets or inserting frames can change the
         * format of the following frames, in case of packed frames. In practice the whole table
         * must be recomputed. For this the frames are marked as potentially invalid. This will
         * cause the whole class to be reread and rewritten with the COMPUTE_FRAMES option (see the
         * ClassWriter.toByteArray method). This is not very efficient but is much easier and
         * requires much less code than any other method I can think of.
         */
        cw.invalidFrames = true;
      }
    }
    // updates the exception handler block labels
    Handler h = firstHandler;
    while(h!=null){
      getNewOffset(allIndexes,allSizes,h.start);
      getNewOffset(allIndexes,allSizes,h.end);
      getNewOffset(allIndexes,allSizes,h.handler);
      h = h.next;
    }
    // updates the instructions addresses in the
    // local var and line number tables
    for(i = 0;i<2;++i){
      ByteVector bv = i==0?localVar:localVarType;
      if(bv!=null){
        b = bv.data;
        u = 0;
        while(u<bv.length){
          label = readUnsignedShort(b,u);
          newOffset = getNewOffset(allIndexes,allSizes,0,label);
          writeShort(b,u,newOffset);
          label += readUnsignedShort(b,u+2);
          newOffset = getNewOffset(allIndexes,allSizes,0,label)-newOffset;
          writeShort(b,u+2,newOffset);
          u += 10;
        }
      }
    }
    if(lineNumber!=null){
      b = lineNumber.data;
      u = 0;
      while(u<lineNumber.length){
        writeShort(b,u,getNewOffset(allIndexes,allSizes,0,readUnsignedShort(b,u)));
        u += 4;
      }
    }
    // updates the labels of the other attributes
    Attribute attr = cattrs;
    while(attr!=null){
      Label[] labels = attr.getLabels();
      if(labels!=null){
        for(i = labels.length-1;i>=0;--i){
          getNewOffset(allIndexes,allSizes,labels[i]);
        }
      }
      attr = attr.next;
    }
    code = newCode;
  }

  static int readUnsignedShort(final byte[] b,final int index){
    return ((b[index]&0xFF)<<8)|(b[index+1]&0xFF);
  }

  static short readShort(final byte[] b,final int index){
    return (short)(((b[index]&0xFF)<<8)|(b[index+1]&0xFF));
  }

  static int readInt(final byte[] b,final int index){
    return ((b[index]&0xFF)<<24)|((b[index+1]&0xFF)<<16)|((b[index+2]&0xFF)<<8)|(b[index+3]&0xFF);
  }

  static void writeShort(final byte[] b,final int index,final int s){
    b[index] = (byte)(s>>>8);
    b[index+1] = (byte)s;
  }

  static int getNewOffset(final int[] indexes,final int[] sizes,final int begin,final int end){
    int offset = end-begin;
    for(int i = 0;i<indexes.length;++i){
      if(begin<indexes[i]&&indexes[i]<=end){
        // forward jump
        offset += sizes[i];
      }else if(end<indexes[i]&&indexes[i]<=begin){
        // backward jump
        offset -= sizes[i];
      }
    }
    return offset;
  }

  static void getNewOffset(final int[] indexes,final int[] sizes,final Label label){
    if((label.status&Label.RESIZED)==0){
      label.position = getNewOffset(indexes,sizes,0,label.position);
      label.status |= Label.RESIZED;
    }
  }
}
