package org.apache.amber.operator.cloudmapper

import com.fasterxml.jackson.annotation.{JsonProperty, JsonPropertyDescription}
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle
import org.apache.texera.amber.core.tuple.{Attribute, AttributeType, Schema}
import org.apache.texera.amber.core.workflow.{OutputPort, PortIdentity}
import org.apache.texera.amber.operator.metadata.{OperatorGroupConstants, OperatorInfo}
import org.apache.texera.amber.operator.source.PythonSourceOperatorDescriptor
import org.apache.texera.amber.core.storage.{DocumentFactory, FileResolver}

class CloudMapperSourceOpDesc extends PythonSourceOperatorDescriptor {
  @JsonProperty(required = true)
  @JsonSchemaTitle("FastQ Dataset")
  @JsonPropertyDescription("Dataset containing fastq files")
  val directoryName: String = ""

  @JsonProperty(required = true)
  var referenceGenome: ReferenceGenome = _

  @JsonProperty(required = false)
  @JsonSchemaTitle("Additional Reference Genomes")
  @JsonPropertyDescription("Add one or more additional reference genomes (optional)")
  var additionalReferenceGenomes: List[ReferenceGenome] = List()

  @JsonProperty(required = true)
  @JsonSchemaTitle("Cluster")
  @JsonPropertyDescription("Cluster")
  val cluster: String = ""

  private var clusterLauncherServiceTarget: String =
    "http://cloudmapper-service.texera.svc.cluster.local:4000"

  // Getter to retrieve only the id part (cid) from the cluster
  def clusterId: String = {
    if (cluster.startsWith("#")) {
      cluster.split(" ")(0).substring(1) // Extracts the cid part by splitting and removing '#'
    } else {
      ""
    }
  }

  override def generatePythonCode(): String = {
    val directoryUri = FileResolver.resolveDirectory(directoryName)
    println(directoryUri.toASCIIString)

    val directoryDocument = DocumentFactory.openReadonlyDocument(directoryUri, isDirectory = true)
    val directoryFile = directoryDocument.asFile()
    println(directoryFile.getAbsolutePath)

    // Convert the Scala referenceGenome to a Python string
    val pythonReferenceGenome = s"'${referenceGenome.referenceGenome.getName}'"

    // Convert the Scala additionalReferenceGenomes list to a Python list format
    val pythonAdditionalReferenceGenomes = additionalReferenceGenomes
      .map(_.referenceGenome.getName)
      .map(name => s"'$name'")
      .mkString("[", ", ", "]")

    // Combine main reference genome with additional ones
    val pythonAllReferenceGenomes =
      s"[${pythonReferenceGenome}] + ${pythonAdditionalReferenceGenomes}"

    // Convert all reference genomes (main + additional) to a Python list format for FASTA files
    val pythonFastaFiles = (referenceGenome :: additionalReferenceGenomes)
      .flatMap(_.fastAFiles)
      .map(file => {
        val fileUri = FileResolver.resolve(file)
        val fileDocument = DocumentFactory.openReadonlyDocument(fileUri, isDirectory = false)
        val fastAFilePath = fileDocument.asFile().getAbsolutePath
        s"open(r'$fastAFilePath', 'rb')"
      })
      .mkString("[", ", ", "]")

    // Extract GTF file if exists for 'My Reference' (considering both main and additional reference genomes)
    val pythonGtfFile = (referenceGenome :: additionalReferenceGenomes)
      .find(_.referenceGenome == ReferenceGenomeEnum.MY_REFERENCE)
      .flatMap(_.gtfFile)
      .map(file => {
        val fileUri = FileResolver.resolve(file)
        val fileDocument = DocumentFactory.openReadonlyDocument(fileUri, isDirectory = false)
        val gtfFilePath = fileDocument.asFile().getAbsolutePath
        s"open(r'$gtfFilePath', 'rb')"
      })
      .getOrElse("None")

    val pythonGtfFileValue = if (pythonGtfFile == "None") "None" else pythonGtfFile

    s"""from pytexera import *
       |
       |class GenerateOperator(UDFSourceOperator):
       |
       |    @overrides
       |    def produce(self) -> Iterator[Union[TupleLike, TableLike, None]]:
       |        import requests, time, json, base64
       |
       |        reads_path = r'${directoryFile.getAbsolutePath}'
       |        service_url = "${clusterLauncherServiceTarget}"
       |        cluster_id  = ${clusterId}
       |
       |        # ------------------------------------------------------------------
       |        # Step 1: Ask the Go service for a presigned S3 PUT URL.
       |        # The reads zip will be sent directly to S3 from here — the Go
       |        # service is not in the data path for the large file.
       |        # ------------------------------------------------------------------
       |        upload_meta_resp = requests.post(f"{service_url}/api/job/request-upload")
       |        upload_meta_resp.raise_for_status()
       |        upload_meta = upload_meta_resp.json()
       |        upload_url = upload_meta["upload_url"]
       |        s3_key     = upload_meta["s3_key"]
       |        job_id     = upload_meta["job_id"]
       |
       |        yield  # let Texera heartbeat while we upload
       |
       |        # ------------------------------------------------------------------
       |        # Step 2: PUT the reads zip directly to S3 (presigned URL, no proxy).
       |        # ------------------------------------------------------------------
       |        with open(reads_path, 'rb') as reads_file:
       |            put_resp = requests.put(upload_url, data=reads_file)
       |        put_resp.raise_for_status()
       |
       |        yield  # let Texera heartbeat while we notify
       |
       |        # ------------------------------------------------------------------
       |        # Step 3: Notify the Go service to start the job. Pass s3_key and
       |        # job_id so it knows which S3 object to pull on the EC2 head node.
       |        # FASTA/GTF files (small, annotation-only) still go as multipart.
       |        # ------------------------------------------------------------------
       |        selected_genomes = ${pythonAllReferenceGenomes}
       |        form_data = {
       |            'cid':    str(cluster_id),
       |            's3_key': s3_key,
       |            'job_id': str(job_id),
       |        }
       |        for index, genome in enumerate(selected_genomes):
       |            form_data[f'referenceGenome[{index}]'] = genome
       |
       |        files = {}
       |        if 'My Reference' in selected_genomes:
       |            fasta_files = ${pythonFastaFiles}
       |            for index, fasta_file in enumerate(fasta_files):
       |                files[f'fastaFiles[{index}]'] = fasta_file
       |            gtf_file = ${pythonGtfFileValue}
       |            if gtf_file is not None:
       |                files['gtfFile'] = gtf_file
       |
       |        response = requests.post(f"{service_url}/api/job/create",
       |                                 data=form_data, files=files if files else None)
       |        response.raise_for_status()
       |
       |        # ------------------------------------------------------------------
       |        # Step 4: Poll until the job is finished.
       |        # ------------------------------------------------------------------
       |        while True:
       |            status_response = requests.get(f'{service_url}/api/job/status/{job_id}')
       |            status = status_response.json().get("status")
       |
       |            if status == "finished":
       |                print("Job finished! Downloading the result...")
       |                break
       |            elif status == "failed":
       |                print("Job failed.")
       |                yield {
       |                    'Sample': None,
       |                    'features.tsv.gz': None,
       |                    'barcodes.tsv.gz': None,
       |                    'matrix.mtx.gz': None
       |                }
       |                return
       |
       |            print("Job is still processing...")
       |            time.sleep(0.5)
       |            yield
       |
       |        # ------------------------------------------------------------------
       |        # Step 5: Download results.
       |        # ------------------------------------------------------------------
       |        download_response = requests.get(f'{service_url}/api/job/download/{job_id}',
       |                                         params={'cid': str(cluster_id)})
       |
       |        if download_response.status_code == 200:
       |            all_file_contents = json.loads(download_response.content)
       |
       |            for subdirectory, file_contents in all_file_contents.items():
       |                features_content = base64.b64decode(file_contents['features.tsv.gz'])
       |                barcodes_content = base64.b64decode(file_contents['barcodes.tsv.gz'])
       |                matrix_content   = base64.b64decode(file_contents['matrix.mtx.gz'])
       |
       |                yield {
       |                    'Sample':          subdirectory,
       |                    'features.tsv.gz': features_content,
       |                    'barcodes.tsv.gz': barcodes_content,
       |                    'matrix.mtx.gz':   matrix_content
       |                }
       |        else:
       |            print(f"Failed to get the files. Status Code: {download_response.status_code}")
       |            print(f"Response Text: {download_response.text}")
       |            yield {
       |                'Sample': None,
       |                'features.tsv.gz': None,
       |                'barcodes.tsv.gz': None,
       |                'matrix.mtx.gz': None
       |            }
    """.stripMargin
  }
  override def operatorInfo: OperatorInfo =
    OperatorInfo(
      "CloudBioMapper",
      "Running sequence alignment using public cluster services",
      OperatorGroupConstants.API_GROUP,
      inputPorts = List.empty,
      outputPorts = List(OutputPort())
    )
  override def asSource() = true
  override def sourceSchema(): Schema =
    Schema()
      .add(
        new Attribute("Sample", AttributeType.STRING),
        new Attribute("features.tsv.gz", AttributeType.BINARY),
        new Attribute("barcodes.tsv.gz", AttributeType.BINARY),
        new Attribute("matrix.mtx.gz", AttributeType.BINARY)
      )

  def getOutputSchemas(inputSchemas: Map[PortIdentity, Schema]): Map[PortIdentity, Schema] = {
    Map(operatorInfo.outputPorts.head.id -> sourceSchema())
  }
}
