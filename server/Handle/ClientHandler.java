package projectOne.server.Handle;

import projectOne.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * @author rafa gao
 */

public class ClientHandler {
    private Socket client;
    private String sn;
    private ClientReadHandle clientReadHandle;
    private ClientWriteHandle clientWriteHandle;
    private CloseNotify closeNotify;

    public ClientHandler(Socket client, String sn, CloseNotify closeNotify) throws IOException {
        this.client = client;
        this.sn = sn;
        this.closeNotify = closeNotify;
        System.out.println("Client" + "[" + sn + "]" + " started:" + "  IP:" + "[" + client.getInetAddress() + "]" + "   PORT" + "[" + client.getPort() + "]");
        clientReadHandle = new ClientReadHandle(client.getInputStream());//抛异常
        clientWriteHandle = new ClientWriteHandle(client.getOutputStream());
    }

    public void send(String str) {
        //利用线程池来发送数据
        clientWriteHandle.send(str);

    }

    public void exit() {
        clientReadHandle.exit();
        clientWriteHandle.exit();
        CloseUtils.close(client);
        //打印退出信息
        System.out.println("Client" + "[" + sn + "]" + " exited:" + "  IP:" + "[" + client.getInetAddress() + "]" + "   PORT" + "[" + client.getPort() + "]");
    }

    public void readToPrint() {
        if (clientReadHandle != null) {
            //开启读取线程即可
            clientReadHandle.start();
        } else {
            System.out.println("clientReadHandle is null");
        }
    }

    private void exitByself() {
        exit();
        //后续操作
        closeNotify.onSelfClosed(this);
    }

    public interface CloseNotify {
        void onSelfClosed(ClientHandler clientHandler);
    }


    //客户端消息读取线程
    private class ClientReadHandle extends Thread {
        //这个线程只需要得到输入流即可
        private final InputStream inputStream;

        private ClientReadHandle(InputStream inputStream) {
            this.inputStream = inputStream;
        }


        @Override
        public void run() {
            super.run();
            try {
                //开始进行接收客户端的消息
                todo(inputStream);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("read data exception");
                ClientHandler.this.exit();
            } finally {
                exit();
            }

        }

        private void todo(InputStream inputStream) throws IOException {
            //构建从客户端得到的流
            BufferedReader bufferedReaderSocket = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                String data = bufferedReaderSocket.readLine();
                //已经无法再读取消息，退出整个客户端
                if (data == null) {
                    System.out.println("can't read data anymore");
                    bufferedReaderSocket.close();
                    ClientHandler.this.exitByself();
                    break;
                }
                System.out.println(data);
            }
            bufferedReaderSocket.close();
        }

        private void exit() {
            CloseUtils.close(inputStream);
        }
    }

    //回送给客户端的线程
    private class ClientWriteHandle {
        private final PrintStream printStream;
        //单个线程的线程池
        private ExecutorService executorService;

        private ClientWriteHandle(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            //创建线程池
            executorService = Executors.newSingleThreadExecutor();
        }


        private void exit() {
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        private void send(String str) {

            executorService.execute(new sendRunnable(str));
        }

        //创建任务类
        private class sendRunnable implements Runnable {
            //要传送的数据
            private final String str;

            private sendRunnable(String str) {
                this.str = str;
            }

            @Override
            public void run() {

                printStream.println(str);
            }
        }
    }
}
