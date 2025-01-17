package wordcount;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import textpre.PorterStemmer;
import textpre.TextPreExe;

public class WordCount {
    /**
     * Mapper区: WordCount程序 Map 类
     * Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>:
     *        |       |           |             |
     *  输入key类型  输入value类型      输出key类型 输出value类型
     * @author johnnie
     *
     */
    public static class TokenizerMapper extends Mapper<LongWritable, Text, Text, IntWritable>{
        // 输出结果
        private Text word = new Text();                             // KEYOUT
        // 因为若每个单词出现后，就置为 1，并将其作为一个<key,value>对，因此可以声明为常量，值为 1
        private final static IntWritable one = new IntWritable(1);  // VALUEOUT

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // 分词：将每行的单词进行分割,按照"  \t\n\r\f"(空格、制表符、换行符、回车符、换页)进行分割
            StringTokenizer tokenizer = new StringTokenizer(value.toString());
            // 遍历
            while (tokenizer.hasMoreTokens()) {
                // 获取每个值
                String wordValue = tokenizer.nextToken();
                // 先对部分特殊单词进行预处理
                String wordValueTemp1 = TextPreExe.dealAbbr(wordValue);
                // 对wordvalue进行过滤（特殊字符 + 数字）
                String wordValueTemp2 = TextPreExe.NumerFilter(TextPreExe.StringFilter(wordValueTemp1));
                if(wordValueTemp2.equals("") == true){ // 如果过滤之后的wordValue为空
                    continue;
                }
                wordValue = new PorterStemmer().stem(wordValueTemp2);
                // 设置 map 输出的 key 值
                word.set(wordValue);
                // 上下文输出 map 处理结果
                context.write(word, one);
            }
        }
    }

    /**
     * Reducer 区域：WordCount 程序 Reduce 类
     * Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>:Map 的输出类型，就是Reduce 的输入类型
     * @author johnnie
     *
     */
    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        // 输出结果：总次数
        private IntWritable result = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;                        // 累加器，累加每个单词出现的总次数
            // 遍历values
            for (IntWritable val : values) {
                sum += val.get();               // 累加
            }
            // 设置输出 value
            result.set(sum);
            // 上下文输出 reduce 结果
            context.write(key, result);
        }
    }

    // Driver 区：客户端
    public static void main(String[] args) throws Exception {
        // 获取配置信息
        Configuration conf = new Configuration();
        // 创建一个 Job
        Job job = Job.getInstance(conf, "word count");      // 设置 job name 为 word count
        // 1. 设置 Job 运行的类
        job.setJarByClass(WordCount.class);

        // 2. 设置Mapper类和Reducer类
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);

        // 3. 获取输入参数，设置输入文件目录和输出文件目录
        /*String[] inputFile = new String[1];
        inputFile[0] = "inputSource/NovelTexts.txt";
        PorterStemmer.porterMain(inputFile);*/

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 4. 设置输出结果 key 和 value 的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
//      job.setCombinerClass(IntSumReducer.class);

        // 5. 提交 job，等待运行结果，并在客户端显示运行信息，最后结束程序
        boolean isSuccess = job.waitForCompletion(true);

        // 结束程序
        System.exit(isSuccess ? 0 : 1);
    }

}