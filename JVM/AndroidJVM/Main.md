#### Android JVM知识目录

* #### Dex文件结构

  * dex
  * odex
  * oat

* #### 虚拟机

  * Dalvik 优化
    * dex通过dex_opt优化成odex
    * 采用JIT
  * ART优化
    * 5.0-7.0通过dex2oat优化成oat文件
    * 7.0之后(三者并存)
      * 解释器
      * JIT
      * OAT

#### <span id="dex">Dex文件结构</span>

dex文件的数据加载到内存对应以下DexFile结构，可以直接通过偏移量访问。

```c
//dalvik/libdex/DexFile.h
struct DexFile {
    /* directly-mapped "opt" header */
    const DexOptHeader* pOptHeader;
	//索引区
    /* pointers to directly-mapped structs and arrays in base DEX */
    const DexHeader*    pHeader;
    const DexStringId*  pStringIds;
    const DexTypeId*    pTypeIds;
    const DexFieldId*   pFieldIds;
    const DexMethodId*  pMethodIds;
    const DexProtoId*   pProtoIds;
    const DexClassDef*  pClassDefs;
    const DexLink*      pLinkData;

    /*
     * These are mapped out of the "auxillary" section, and may not be
     * included in the file.
     */
    const DexClassLookup* pClassLookup;
    const void*         pRegisterMapPool;       // RegisterMapClassPool
	//dex文件加载到内存以字节数组的形式存在，该指针指向首地址
    /* points to start of DEX file data */
    const u1*           baseAddr;

    /* track memory overhead for auxillary structures */
    int                 overhead;

    /* additional app-specific data structures associated with the DEX */
    //void*               auxData;
};
```

* DexOptHeader

  ```c
  /*
   * Header added by DEX optimization pass.  Values are always written in
   * local byte and structure padding.  The first field (magic + version)
   * is guaranteed to be present and directly readable for all expected
   * compiler configurations; the rest is version-dependent.
   *
   * Try to keep this simple and fixed-size.
   */
  struct DexOptHeader {
      u1  magic[8];           /* includes version number */
  
      u4  dexOffset;          /* file offset of DEX header */
      u4  dexLength;
      u4  depsOffset;         /* offset of optimized DEX dependency table */
      u4  depsLength;
      u4  optOffset;          /* file offset of optimized data tables */
      u4  optLength;
  
      u4  flags;              /* some info flags */
      u4  checksum;           /* adler32 checksum covering deps/opt */
  
      /* pad for 64-bit alignment if necessary */
  };
  ```

* DexHeader

  ```c
  /*
   * Direct-mapped "header_item" struct.
   */
  struct DexHeader {
      u1  magic[8];           /* includes version number */
      u4  checksum;           /* adler32 checksum */
      u1  signature[kSHA1DigestLen]; /* SHA-1 hash */
      u4  fileSize;           /* length of entire file */
      u4  headerSize;         /* offset to start of next section */
      u4  endianTag;
      u4  linkSize;
      u4  linkOff;
      u4  mapOff;
      u4  stringIdsSize;
      u4  stringIdsOff;
      u4  typeIdsSize;
      u4  typeIdsOff;
      u4  protoIdsSize;
      u4  protoIdsOff;
      u4  fieldIdsSize;
      u4  fieldIdsOff;
      u4  methodIdsSize;
      u4  methodIdsOff;
      u4  classDefsSize;
      u4  classDefsOff;
      u4  dataSize;
      u4  dataOff;
  };
  
  ```

* DexStringId

  ```c
  /*
   * Direct-mapped "string_id_item".
   */
  struct DexStringId {
      u4 stringDataOff;      /* file offset to string_data_item */
  };
  ```

* DexTypeId

  ```c
  /*
   * Direct-mapped "type_id_item".
   */
  struct DexTypeId {
      u4  descriptorIdx;      /* index into stringIds list for type descriptor */
  };
  ```

  

* DexFieldId

  ```c
  /*
   * Direct-mapped "field_id_item".
   */
  struct DexFieldId {
      u2  classIdx;           /* index into typeIds list for defining class */
      u2  typeIdx;            /* index into typeIds for field type */
      u4  nameIdx;            /* index into stringIds for field name */
  };
  ```

* DexMethodId

  ```c
  /*
   * Direct-mapped "method_id_item".
   */
  struct DexMethodId {
      u2  classIdx;           /* index into typeIds list for defining class */
      u2  protoIdx;           /* index into protoIds for method prototype */
      u4  nameIdx;            /* index into stringIds for method name */
  };
  ```

* DexProtoId

  ```c
  /*
   * Direct-mapped "proto_id_item".
   */
  struct DexProtoId {
      u4  shortyIdx;          /* index into stringIds for shorty descriptor */
      u4  returnTypeIdx;      /* index into typeIds list for return type */
      u4  parametersOff;      /* file offset to type_list for parameter types */
  };
  ```

* DexClassDef

  ```c
  /*
   * Direct-mapped "class_def_item".
   */
  struct DexClassDef {
      u4  classIdx;           /* index into typeIds for this class */
      u4  accessFlags;
      u4  superclassIdx;      /* index into typeIds for superclass */
      u4  interfacesOff;      /* file offset to DexTypeList */
      u4  sourceFileIdx;      /* index into stringIds for source file name */
      u4  annotationsOff;     /* file offset to annotations_directory_item */
      u4  classDataOff;       /* file offset to class_data_item */
      u4  staticValuesOff;    /* file offset to DexEncodedArray */
  };
  ```

#### <span id="jvm">虚拟机</span>

Android虚拟机是基于寄存器，所以速度要快于基于栈的Java虚拟机。从早期的Dalvik虚拟机发展到现在的ART虚拟机(Android 5.0及以后默认虚拟机)。

##### ART优化

从5.0到7.0版本在安装时，PMKS会通过dex2oat静态方式编译dex文件生成oat，所以很耗时。

从7.0版本之后采用混合模式，即采用解释器+JIT+OAT的方式，系统会再空闲的时候将dex编译成oat。

[详细请参考](https://www.jianshu.com/p/bcc4a9209ef5)









