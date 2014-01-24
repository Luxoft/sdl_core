package com.ford.syncV4.syncConnection;

import android.test.InstrumentationTestCase;

import com.ford.syncV4.protocol.ProtocolFrameHeader;
import com.ford.syncV4.protocol.ProtocolFrameHeaderFactory;
import com.ford.syncV4.protocol.WiProProtocol;
import com.ford.syncV4.protocol.enums.ServiceType;
import com.ford.syncV4.session.Session;
import com.ford.syncV4.streaming.H264Packetizer;
import com.ford.syncV4.transport.SyncTransport;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.transport.TransportType;
import com.ford.syncV4.util.BitConverter;

import org.mockito.ArgumentCaptor;

import java.io.OutputStream;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Andrew Batutin on 8/22/13.
 */
public class SyncConnectionTest extends InstrumentationTestCase {

    public static final byte VERSION = (byte) 2;
    public static final byte SESSION_ID = (byte) 48;
    public static final int MESSAGE_ID = 48;
    private SyncConnection sut;
    private TCPTransportConfig config;

    public SyncConnectionTest() {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        config = mock(TCPTransportConfig.class);
        when(config.getTransportType()).thenReturn(TransportType.TCP);
        sut = new SyncConnection(mock(ISyncConnectionListener.class), config);
        WiProProtocol protocol = (WiProProtocol) sut.getWiProProtocol();
        protocol.setVersion(VERSION);
    }

    public void testSyncConnectionShouldBeCreated() throws Exception {
        SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        assertNotNull("should not be null", connection);
    }

    public void testStartMobileNavServiceShouldSendAppropriateBytes() throws Exception {
        final boolean[] passed = {false};
        byte sessionID = 0x0A;
        Session session = new Session();
        session.setSessionId(sessionID);
        ProtocolFrameHeader header = ProtocolFrameHeaderFactory.createStartSession(ServiceType.Mobile_Nav, 0x00, VERSION);
        header.setSessionID(sessionID);
        final ProtocolFrameHeader realHeader = header;
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {

            @Override
            public void onProtocolMessageBytesToSend(byte[] msgBytes, int offset,
                                                     int length) {
                super.onProtocolMessageBytesToSend(msgBytes, offset, length);
                assertTrue("Arrays should be equal", Arrays.equals(msgBytes, realHeader.assembleHeaderBytes()));
                assertEquals("Offset should be 0", offset, 0);
                assertEquals("Length should be 12", length, 12);
                passed[0] = true;
            }
        };
        WiProProtocol protocol = (WiProProtocol) connection.getWiProProtocol();
        protocol.setVersion(VERSION);
        connection.startMobileNavService(session);
        assertTrue(passed[0]);
    }

    public void testOnTransportBytesReceivedReturnedStartSessionACK() throws Exception {
        final boolean[] passed = {false};
        final ProtocolFrameHeader header = ProtocolFrameHeaderFactory.createStartSessionACK(ServiceType.Mobile_Nav, SESSION_ID, MESSAGE_ID, VERSION);
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {

            @Override
            public void onProtocolServiceStarted(ServiceType serviceType, byte sessionID, byte version, String correlationID) {
                super.onProtocolServiceStarted(serviceType,sessionID, version, correlationID);
                assertEquals("Correlation ID is empty string so far", "", correlationID);
                assertEquals("ServiceType should be equal.", header.getServiceType(), serviceType);
                assertEquals("Frame headers should be equal.", header.getSessionID(), sessionID);
                assertEquals("Version should be equal.", header.getVersion(), version);
                passed[0] = true;
            }
        };
        WiProProtocol protocol = (WiProProtocol) connection.getWiProProtocol();
        protocol.setVersion(VERSION);
        connection.onTransportBytesReceived(header.assembleHeaderBytes(), header.assembleHeaderBytes().length);
        assertTrue(passed[0]);
    }

    public void testCloseMobileNavSessionShouldSendAppropriateBytes() throws Exception {
        byte[] data = BitConverter.intToByteArray(0);
        final ProtocolFrameHeader header = ProtocolFrameHeaderFactory.createEndSession(ServiceType.Mobile_Nav, SESSION_ID, 0, VERSION, data.length);
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {

            private int count = 0;

            @Override
            public void closeMobileNaviService(byte rpcSessionID) {
                _transport = mock(SyncTransport.class);
                when(_transport.getIsConnected()).thenReturn(true);
                super.closeMobileNaviService(rpcSessionID);
            }

            @Override
            public void onProtocolMessageBytesToSend(byte[] msgBytes, int offset,
                                                     int length) {
                super.onProtocolMessageBytesToSend(msgBytes, offset, length);
                if (count == 0) {
                    assertTrue("Arrays should be equal", Arrays.equals(msgBytes, header.assembleHeaderBytes()));
                    assertEquals("Offset should be 0", offset, 0);
                    assertEquals("Length should be 12", length, 12);
                    count++;
                }
            }
        };
        WiProProtocol protocol = (WiProProtocol) connection.getWiProProtocol();
        protocol.setVersion(VERSION);
        connection.closeMobileNaviService(SESSION_ID);
    }

    public void testStopTransportIsCalledForRPCService() throws Exception {
        SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {
            @Override
            public Boolean getIsConnected() {
                _transport = mock(SyncTransport.class);
                return super.getIsConnected();
            }
        };
        connection.getIsConnected();
        connection.onProtocolServiceEnded(ServiceType.RPC, SESSION_ID, "");
        verify(connection._transport, times(1)).stopReading();
    }

    public void testStopTransportNotCalledForNavigationService() throws Exception {
        SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {
            @Override
            public Boolean getIsConnected() {
                _transport = mock(SyncTransport.class);
                return super.getIsConnected();
            }
        };
        connection.getIsConnected();
        connection.onProtocolServiceEnded(ServiceType.Mobile_Nav, SESSION_ID, "");
        verify(connection._transport, never()).stopReading();

    }

    public void testStartAudioServiceShouldSendAppropriateBytes() throws Exception {
        final boolean[] isPassed = {false};
        byte sessionID = 0x0A;
        Session session = new Session();
        session.setSessionId(sessionID);
        ProtocolFrameHeader header = ProtocolFrameHeaderFactory.createStartSession(ServiceType.Audio_Service, 0x00, VERSION);
        header.setSessionID(sessionID);
        final ProtocolFrameHeader realHeader = header;
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config) {

            @Override
            public void onProtocolMessageBytesToSend(byte[] msgBytes, int offset,
                                                     int length) {
                super.onProtocolMessageBytesToSend(msgBytes, offset, length);
                isPassed[0] = true;
                assertTrue("Arrays should be equal", Arrays.equals(msgBytes, realHeader.assembleHeaderBytes()));
                assertEquals("Offset should be 0", offset, 0);
                assertEquals("Length should be 12", length, 12);
            }
        };
        WiProProtocol protocol = (WiProProtocol) connection.getWiProProtocol();
        protocol.setVersion(VERSION);
        connection.startAudioService(session);
        assertTrue(isPassed[0]);
    }

    public void testStartAudioDataTransferReturnsOutputStream() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        OutputStream stream = connection.startAudioDataTransfer(SESSION_ID);
        assertNotNull("output stream should be created", stream);
    }

    public void testStartAudioDataTransferCreatesAudioPacketizer() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        OutputStream stream = connection.startAudioDataTransfer(SESSION_ID);
        assertNotNull("audio pacetizer should not be null", connection.mAudioPacketizer);
    }

    public void testStartAudioDataTransferStartsPacetizer() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        OutputStream stream = connection.startAudioDataTransfer(SESSION_ID);
        H264Packetizer packetizer = (H264Packetizer) connection.mAudioPacketizer;
        assertEquals(Thread.State.RUNNABLE, packetizer.getThread().getState());
    }

    public void testStartAudioDataTransferSetsSessionID() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        OutputStream stream = connection.startAudioDataTransfer(SESSION_ID);
        H264Packetizer packetizer = (H264Packetizer) connection.mAudioPacketizer;
        assertEquals("session id should be equal SESSION_ID", SESSION_ID, packetizer.getSessionID());
    }

    public void testStopAudioDataTransferStopPacketizer() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        connection.mAudioPacketizer = mock(H264Packetizer.class);
        connection.stopAudioDataTransfer();
        verify(connection.mAudioPacketizer, times(1)).stop();
    }

    public void testCloseAudioServiceSendEndServiceMessage() throws Exception {
        final SyncConnection connection = new SyncConnection(mock(ISyncConnectionListener.class), config);
        connection._protocol = mock(WiProProtocol.class);
        connection._transport = mock(SyncTransport.class);
        when(connection._transport.getIsConnected()).thenReturn(true);
        connection.closeAudioService(SESSION_ID);
        ArgumentCaptor<ServiceType> serviceTypeCaptor = ArgumentCaptor.forClass(ServiceType.class);
        ArgumentCaptor<Byte> sessionIDCaptor = ArgumentCaptor.forClass(byte.class);
        verify(connection._protocol, times(1)).EndProtocolService(serviceTypeCaptor.capture(), sessionIDCaptor.capture());
        assertEquals("should end audio service", ServiceType.Audio_Service, serviceTypeCaptor.getValue());
        assertEquals("should end session with SESSION_ID", SESSION_ID, sessionIDCaptor.getValue().byteValue());
    }
}
