package com.chuckstechtalk.alchitry;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

/**
 * Support reading and writing over serial port to Alchitry Au
 * via a register interface protocol.
 */
public class RegisterInterface implements AutoCloseable {
	private SerialPort serialPort;
	private final Semaphore semaphore;

	public RegisterInterface() {
		// Create a binary semaphore to protect concurrent access 
		// to the serial port.
		semaphore = new Semaphore(1);
	}

	/**
	 * Connect to the Alchitry board on the given port
	 * and at the given baud rate.
	 * 
	 * @param port	The com port to connect to.
	 * @param baud	The baud rate to connect at.
	 * @return	True if connect was successful.
	 */
	public boolean connect(String port, int baud) {
		if (port == null)
			return false;

		if (port.equals(""))
			return false;
		if (!Arrays.asList(SerialPortList.getPortNames()).contains(port))
			return false;

		serialPort = new SerialPort(port);
		try {
			serialPort.openPort();
		} catch (SerialPortException e) {
			return false;
		}
		try {
			serialPort.setParams(baud, 8, 1, 0);
		} catch (SerialPortException e) {
			try {
				serialPort.closePort();
			} catch (SerialPortException e1) {
				e1.printStackTrace();
			}
			return false;
		}

		return true;
	}

	/**
	 * Disconnect serial port from Alchitry board.
	 * @return	True if successful.
	 * @throws SerialPortException
	 */
	public boolean disconnect() throws SerialPortException {
		return serialPort.closePort();
	}

	/**
	 * Write an integer to an address.
	 * @param address	An identifier used by the FPGA to classify a data request.
	 * @param data		The data to write.
	 * @return				True if write sucessful.
	 * @throws SerialPortException
	 * @throws InterruptedException
	 */
	public boolean write(int address, int data) throws SerialPortException, InterruptedException {
		byte[] buff = new byte[9];
		buff[0] = (byte) (1 << 7);
		buff[1] = (byte) (address & 0xff);
		buff[2] = (byte) ((address >> 8) & 0xff);
		buff[3] = (byte) ((address >> 16) & 0xff);
		buff[4] = (byte) ((address >> 24) & 0xff);
		buff[5] = (byte) (data & 0xff);
		buff[6] = (byte) ((data >> 8) & 0xff);
		buff[7] = (byte) ((data >> 16) & 0xff);
		buff[8] = (byte) ((data >> 24) & 0xff);
		semaphore.acquire();
		try {
			return serialPort.writeBytes(buff);
		} finally {
			semaphore.release();
		}
	}

	/**
	 * Write multiple integers to a fixed or incrementing address.
	 * @param address		An identifier used by the FPGA to classify a data request.
	 * @param increment	If true, increment the address for every write. If false, write to the same address for every int.
	 * @param data			An array of integers to write
	 * @return					True if write successful.
	 * @throws SerialPortException
	 * @throws InterruptedException
	 */
	public boolean write(int address, boolean increment, int[] data) throws SerialPortException, InterruptedException{
		for (int i = 0; i < data.length; i += 64) {
			int length = Math.min(data.length - i, 64);
			if (!write64(address, increment, data, i, length))
				return false;
			if (increment)
				address += length;
		}
		return true;
	}

	private boolean write64(int address, boolean increment, int[] data, int start, int length) throws SerialPortException, InterruptedException {
		byte[] buff = new byte[5 + length * 4];
		buff[0] = (byte) ((1 << 7) | (length - 1));
		if (increment)
			buff[0] |= (1 << 6);
		buff[1] = (byte) (address & 0xff);
		buff[2] = (byte) ((address >> 8) & 0xff);
		buff[3] = (byte) ((address >> 16) & 0xff);
		buff[4] = (byte) ((address >> 24) & 0xff);
		for (int i = 0; i < length; i++) {
			buff[i * 4 + 5] = (byte) (data[i + start] & 0xff);
			buff[i * 4 + 6] = (byte) ((data[i + start] >> 8) & 0xff);
			buff[i * 4 + 7] = (byte) ((data[i + start] >> 16) & 0xff);
			buff[i * 4 + 8] = (byte) ((data[i + start] >> 24) & 0xff);
		}

		semaphore.acquire();
		try {
			return serialPort.writeBytes(buff);
		} finally {
			semaphore.release();
		}
	}

	/**
	 * Read an integer at a given address.
	 * @param address			An identifier used by the FPGA to classify a data request.
	 * @return						The data read as an integer.
	 * @throws SerialPortException				Thrown if address to read from could not be written.
	 * @throws SerialPortTimeoutException	Thrown if no reply in 1000 ms.
	 * @throws InterruptedException
	 */
	public int read(int address) throws SerialPortException, SerialPortTimeoutException, InterruptedException {
		byte[] buff = new byte[5];
		buff[0] = (byte) (0 << 7);
		buff[1] = (byte) (address & 0xff);
		buff[2] = (byte) ((address >> 8) & 0xff);
		buff[3] = (byte) ((address >> 16) & 0xff);
		buff[4] = (byte) ((address >> 24) & 0xff);
		semaphore.acquire();
		try {
			if (!serialPort.writeBytes(buff))
				throw new SerialPortException(serialPort.getPortName(), "readReg", "Failed to write address");
			buff = serialPort.readBytes(4, 1000);
		} finally {
			semaphore.release();
		}
		return (buff[0] & 0xff) | (buff[1] & 0xff) << 8 | (buff[2] & 0xff) << 16 | (buff[3] & 0xff) << 24;
	}

	/**
	 * Read multiple integers from the same or incrementing address.
	 * @param address		The identifier used by the FPGA to classify a data request.
	 * @param increment	If true, increment the address for eveyr read.
	 * @param data			The resulting data read. Preallocate to largest size expected.
	 * @throws SerialPortException
	 * @throws SerialPortTimeoutException
	 * @throws InterruptedException
	 */
	public void read(int address, boolean increment, int[] data) throws SerialPortException, SerialPortTimeoutException, InterruptedException {
		for (int i = 0; i < data.length; i += 64) {
			int length = Math.min(data.length - i, 64);
			read64(address, increment, data, i, length);
			if (increment)
				address += length;
		}
	}

	private void read64(int address, boolean increment, int[] data, int start, int length) throws SerialPortException, SerialPortTimeoutException, InterruptedException {
		byte[] buff = new byte[5];
		buff[0] = (byte) ((0 << 7) | (length - 1));
		if (increment)
			buff[0] |= (1 << 6);
		buff[1] = (byte) (address & 0xff);
		buff[2] = (byte) ((address >> 8) & 0xff);
		buff[3] = (byte) ((address >> 16) & 0xff);
		buff[4] = (byte) ((address >> 24) & 0xff);

		semaphore.acquire();
		try {
			if (!serialPort.writeBytes(buff))
				throw new SerialPortException(serialPort.getPortName(), "readReg", "Failed to write address");

			buff = serialPort.readBytes(length * 4, 3000);
		} finally {
			semaphore.release();
		}

		for (int i = 0; i < buff.length; i += 4) {
			data[i / 4 + start] = (buff[i] & 0xff) | (buff[i + 1] & 0xff) << 8 | (buff[i + 2] & 0xff) << 16 | (buff[i + 3] & 0xff) << 24;
		}
	}

	/**
	 * Disconnect from serial port if using try-with-resources.
	 */
	@Override
	public void close() throws Exception {
		disconnect();
	}
}
