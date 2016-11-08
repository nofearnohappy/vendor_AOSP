#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include "LogDefine.h"
#include "UsbRxWatcher.h"
#include "Modem.h"
#include "Context.h"
#include "PortInterface.h"
#include "FtModule.h"




typedef struct _MetaFrame
{
	unsigned char start;
	unsigned char frmLen[2];
	unsigned char frmType;
	unsigned char localLen[2];
	unsigned char peerLen[2];
	unsigned char token[2];
	unsigned char reqId[2];

}MetaFrame;


UsbRxWatcher::UsbRxWatcher(void)
{
	m_checksum = STX_OCTET;
	m_uFrameLength = 0;
	m_frame_buf_len = 0;
	m_flow_ctrl_flag = 0;
	m_cTstFrameState = RS232_FRAME_STX;
	m_cOldTstFrameState = RS232_FRAME_STX;
	
	nRemainLen = 0;
	m_nStartByteLen = 0;
	m_frm_len_byte = 2;
	m_md_index = 0;
	memset(&m_sRs232Frame, 0, sizeof(m_sRs232Frame));

}

UsbRxWatcher::~UsbRxWatcher(void)
{
	if (m_frame_buf_len > 0)
	{
		free(m_sRs232Frame.buf_ptr);
	}

	//delete ti
}

signed int UsbRxWatcher::onReceived(
							 unsigned char *buf,
							 unsigned int len)
{

	META_LOG("[Meta] Receive data from USB, len = %d",len);
	dumpDataInHexString(buf,len,16);
	META_LOG("[Meta] nRemainLen = %d", nRemainLen);
	
	unsigned char * data = NULL;
	unsigned short u16Length = 0;

	if(nRemainLen > 0)
	{
		bcopy(buf, szRemainBuf+nRemainLen, len);  //Fix for memcpy do not support src/dst overlap case
		data = szRemainBuf;
		len = nRemainLen + len;
	}
	else
	{
		data = buf;
	}

	Frame *pFrm = decode(data, len, u16Length);

	if (NULL != pFrm)
	{
		if(pFrm->getIsValid() == 1)
		{
			pFrm->exec();
			if(len > u16Length)
			{
				bcopy(data+u16Length, szRemainBuf, len-u16Length);  //Fix for memcpy do not support src/dst overlap case
				nRemainLen = 0;
				onReceived(szRemainBuf, len-u16Length);
			}
			else if(len == u16Length)
			{
				nRemainLen = 0;
			}
		}
		else
		{
			META_LOG("[Meta] Unsupport request id");
			if(len == u16Length)
			{
				nRemainLen = 0;
			}
			else if(len > u16Length)
			{
				bcopy(data+u16Length, szRemainBuf, len-u16Length);	//Fix for memcpy do not support src/dst overlap case
				nRemainLen = len-u16Length;
			}
		}
		delete pFrm;
	}
	else
	{
		META_LOG("[Meta] pFrm is NULL");
		if(len == u16Length)
		{
			nRemainLen = len;
			bcopy(data, szRemainBuf, len);  //Fix for memcpy do not support src/dst overlap case 
			META_LOG("[Meta] Data is incomplete, received = %d",len);
		}
		else
		{
			META_LOG("[Meta] Discard incomplete frame %d bytes",u16Length);
			nRemainLen = 0;
			onReceived(data+(u16Length-1), len-(u16Length-1));
		}
	}
	
	return 0;
}

//////////////////////////////////////////////////////////////////////////
unsigned char *UsbRxWatcher::reallocFrameBuf(unsigned int len)
{
	unsigned char *buf_ptr = m_sRs232Frame.buf_ptr;

	META_LOG("[Meta] (reallocFrameBuf) len = %d, m_frame_buf_len=%d",len, m_frame_buf_len);

	if (buf_ptr != NULL)
	{
		free(buf_ptr);
	}

	buf_ptr = (unsigned char *)malloc(len);
	memset(buf_ptr,'\0',len);
	m_frame_buf_len = len;

	return buf_ptr;
}

unsigned int UsbRxWatcher::flowControl(void *pdata, unsigned int len)
{
	/* if the data is 0x77 and 0x01, escape to 0x11
	if the data is 0x77 and 0x02, escape to 0x13
	if the data is 0x77 and 0x77, escape to 0x77
	*/
	if (getComType() == META_UART_COM)
	{
		unsigned int idx = 0;
		unsigned int newLen = 0;
		unsigned char *pTempBuf = (unsigned char*)pdata;
		unsigned char *pDestBuf = pTempBuf;

		while (idx != len)
		{
			++ idx;

			if (idx == len)
			{
				META_LOG("[Meta] root cause1:cnt:%d",idx);
				if(*pTempBuf ==0x77)
				{
					m_flow_ctrl_flag = 1;
					break;
				}
			}

			if (*pTempBuf ==0x77 || m_flow_ctrl_flag != 0)
			{
				if (m_flow_ctrl_flag != 0)
				{
					m_flow_ctrl_flag = 0;
				}
				else
				{
					++ idx;
					++ pTempBuf;
				}

				if (*pTempBuf ==0x01)
				{
					*pDestBuf = 0x11;
				}
				else if (*pTempBuf ==0x02 )
				{
					*pDestBuf = 0x13;
				}
				else if (*pTempBuf ==0x03 )
				{
					*pDestBuf = 0x77;
				}
				else
				{
					META_LOG("[Meta] root cause2: cnt:%d",idx);
					break;
				}
			}
			else
			{
				*pDestBuf = *pTempBuf;
			}

			++ newLen;
			++ pTempBuf;
			++ pDestBuf;
		}
		len = newLen;
	}
	return len;
}

Frame *UsbRxWatcher::decodeMDFrame(void *pdata, unsigned int len,unsigned char frmType,unsigned short &u16Length)
{
	assert(pdata != NULL);


	int bFrm = false;
	unsigned char * local_ptr = (unsigned char*)pdata + u16Length;
	unsigned char	ch;
	int newLen = m_uFrameLength;
		
	while(u16Length != len)
	{
		++u16Length;
		ch = *local_ptr;

		if(ch == MUX_KEY_WORD)
		{
			if(u16Length == len)
				return NULL;
			
			++u16Length;
			++local_ptr;
			ch = *local_ptr;
			if((ch == MUX_KEY_WORD) || (ch == 0x01))
				newLen ++;
		}

		if(getComType() == META_UART_COM && ch == 0x77)
		{
			if(u16Length == len)
				return NULL;
			
			++u16Length;
			++local_ptr;
			ch = *local_ptr;
			if((ch == 0x01) || (ch == 0x02) || (ch == 0x03))
				newLen ++;
		}

		if(ch == STX_OCTET)
		{
			META_LOG("[Meta] New modem frame header");
			bFrm = true;
			u16Length--;
			
		}
		else if(u16Length == newLen + 3 + m_frm_len_byte + m_nStartByteLen)   // 3 + m_frm_len_byte = start byte  + data type + checksum + data len(m_frm_len_byte)
		{
			
			META_LOG("[Meta] New modem frame tail, m_nStartByteLen:%d",m_nStartByteLen);
			bFrm = true;
		}

		if(bFrm)
		{
			len = flowControl(pdata, u16Length);
			return sendMdTask((unsigned char*)pdata + m_nStartByteLen, len - m_nStartByteLen, frmType);
		}
		
		++local_ptr;
	}

	return NULL;			
}

Frame * UsbRxWatcher::sendMdTask(void *pdata, unsigned int len,unsigned char frmType)
{
	assert(pdata != NULL);
	Frame * pFrame = NULL;

	unsigned char mdId = 0;
	
	if ((frmType == RS232_INJECT_PRIMITIVE_OCTET)|| (frmType == RS232_COMMAND_TYPE_OCTET))//0x64 0x63
	{
		mdId = 0;
		//TODO
	}
	else if(frmType >= RS232_INJECT_PRIMITIVE_OCTETMODEM2 && frmType <= RS232_INJECT_PRIMITIVE_OCTETMODEM2_END)//0xA0 ~ A7
	{
		mdId = m_md_index;	
	}
	else if(frmType >= RS232_COMMAND_TYPE_MD2_MEMORY_DUMP && frmType<= RS232_COMMAND_TYPE_MD2_MEMORY_DUMP_END)//0xC0 ~ 0xC7
	{
		mdId = m_md_index;
	}
	else if(frmType >= RS232_COMMAND_TYPE_MD_DATA_TUNNEL_START && frmType <= RS232_COMMAND_TYPE_MD_DATA_TUNNEL_END)//0xD0 ~ 0xD7
	{
		mdId = m_md_index;
	}

	CmdTarget *md = getModem(mdId);

	META_LOG("[Meta] Send data to modem[%d], data len:%d", mdId,len);
	
	dumpDataInHexString((const unsigned char *)pdata,len,16);
	

	if (NULL != md)
	{
		META_RX_DATA metaRxData =
		{
			MD_FRAME, (unsigned char*)pdata, len, 0
		};

		pFrame = new Frame(metaRxData, md);
	}
	else
	{
		pFrame = new Frame();
	}
	return pFrame;
}

Frame *UsbRxWatcher::sendFtTask()
{
	/* send data FT task */
	Frame * pFrame = NULL;
 
	const unsigned int localLen = m_sRs232Frame.inject_prim.local_len;
	const unsigned int peerLen  = m_sRs232Frame.inject_prim.peer_len;

	META_LOG("[Meta] Send data to FT module. localLen = %d, peerLen = %d", localLen, peerLen);

	unsigned char *localBuf = m_sRs232Frame.buf_ptr;
	unsigned char *peerBuf  = localBuf + localLen; 

	const FT_H *hdr = (FT_H*)localBuf;
	META_LOG("[Meta] token = %d, id = %d", hdr->token, hdr->id);

	CmdTarget *mod = getModule(hdr->id);

	if (mod != NULL)
	{
		mod->setToken( hdr->token);
		META_RX_DATA metaRxData =
		{
			AP_FRAME,
			localBuf,
			localLen,
			peerLen
		};

		pFrame = new Frame(metaRxData, mod);
	}
	else
	{
		pFrame = new Frame();
	}

	return pFrame;
}

Frame *UsbRxWatcher::decodeAPFrame( unsigned int input_len,unsigned char * src,unsigned short &u16Length)
{
	unsigned char * local_ptr = src;
	unsigned char	ch;
	
	while(u16Length!=input_len)//request frame length don't include checksum
	{
		++ u16Length;
		ch = *local_ptr;

		if ( ch == STX_OCTET )
		{
			META_LOG("[Meta] New APP frame header");
			return NULL;
		}
		
		if (1 == checkEscape(ch))
		{	
			if(u16Length == input_len)
				return NULL;
			
			ch = *(++local_ptr);
			if(transferFrame(&ch) != 1)
			{
				ch = *(--local_ptr);
			}
			else
			{
				++ u16Length;
			}
		}
		
		if (m_cTstFrameState == RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER)/* fill data to tst_primitive_header_struct */
		{			
			++ m_sRs232Frame.received_prig_header_length;
			*(m_sRs232Frame.header_ptr++) = ch;

			if (m_sRs232Frame.received_prig_header_length == sizeof(PRIM_HEADER))
			{
				if (m_sRs232Frame.inject_prim.local_len != 0)
				{
					m_cTstFrameState = RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA;

					META_LOG("[Meta] RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER: LOCAL len: %d m_peer_len : %d ",
						m_sRs232Frame.inject_prim.local_len, m_sRs232Frame.inject_prim.peer_len);

					m_sRs232Frame.buf_ptr = reallocFrameBuf(m_sRs232Frame.inject_prim.local_len + m_sRs232Frame.inject_prim.peer_len+1);
				}
			}
		}			
		else if(m_cTstFrameState == RS232_FRAME_AP_PRIM_LOCAL_PARA_DATA)/* fill the primitive body to local parameter buffer and peer buffer */
		{
			if (m_sRs232Frame.buf_ptr != NULL)
			{
				*(m_sRs232Frame.buf_ptr + m_sRs232Frame.received_buf_para_length) = ch;
			}

			++ m_sRs232Frame.received_buf_para_length;

			if ((m_sRs232Frame.inject_prim.local_len + m_sRs232Frame.inject_prim.peer_len) == m_sRs232Frame.received_buf_para_length)
			{
				m_cTstFrameState = RS232_FRAME_CHECKSUM;
			}
		}
		else if(m_cTstFrameState == RS232_FRAME_CHECKSUM)
		{	
			META_LOG("[Meta] parse state: RS232_FRAME_CHECKSUM: checksum: %d, ch: %d",m_checksum, ch);
			m_cTstFrameState = RS232_FRAME_STX;
			if(m_checksum != ch)
			{
				META_LOG("[Meta] Checksum error!");
				return (new Frame());
			}
					
			m_sRs232Frame.received_buf_para_length = 0;
			m_checksum = STX_OCTET;
			META_LOG("[Meta] (decodeAPFrame) u16Length = %d", u16Length);
			return sendFtTask();
		}
		
		++local_ptr;	
		m_checksum ^= ch; 
	}
	return NULL;
}


Frame *UsbRxWatcher::decode(
	unsigned char *buf_ptr,
	unsigned int input_len,
	unsigned short &u16Length)
{
	unsigned char	*src=buf_ptr;
	unsigned char	ch;
	unsigned int discard_word=0;
	
	META_LOG("[Meta](UsbRxWatcher) To decode data");
	dumpDataInHexString(buf_ptr,input_len,16);  

	while (u16Length != input_len)
	{
		ch = *src;
		++ u16Length;

		if ( ch == STX_OCTET )
		{
			m_cTstFrameState = RS232_FRAME_LENHI;
			++ src;
			m_uFrameLength = 0;
			m_frm_len_byte = 2;
			m_checksum = STX_OCTET;
			m_nStartByteLen = u16Length - 1;

			if (discard_word > 0)
			{
				META_LOG("[Meta] Discards %d chars.", discard_word);
				discard_word = 0;
			}
			continue;
		}
		else if(m_cTstFrameState == RS232_FRAME_STX)
		{
			++ discard_word;
			++ src;
			continue;
		}
		else
		{
			if (1 == checkEscape(ch))
			{	
				ch = *(++src);
				if(transferFrame(&ch) != 1)
				{
					ch = *(--src);
				}
				else
				{
					if((m_cTstFrameState == RS232_FRAME_LENHI) || (m_cTstFrameState == RS232_FRAME_LENLO))
					{
						m_frm_len_byte += 1;
					}
					++ u16Length;
				}
			}
		}

		switch (m_cTstFrameState)
		{
		
		case RS232_FRAME_LENHI:		
			m_uFrameLength = ch << 8;
			m_cTstFrameState = RS232_FRAME_LENLO;
			META_LOG("[Meta] parse state: RS232_FRAME_LENHI: %x", ch);
			break;

		case RS232_FRAME_LENLO:
			m_uFrameLength += ch;
			m_cTstFrameState = RS232_FRAME_TYPE;
			META_LOG("[Meta] parse state: RS232_FRAME_LENLO: %x, total: %d",ch, m_uFrameLength);
			if ((m_uFrameLength+5) > FRAME_MAX_LEN)
			{
				META_LOG("[Meta] parse state: Error: Frame size is %d+5, exceeds limit of %d.",m_uFrameLength, FRAME_MAX_LEN);
				return NULL;
			}
			break;

		case RS232_FRAME_TYPE:	
			m_checksum ^= ch; 
			META_LOG("[Meta] (decode) u16Length = %d",u16Length);
			return dispatchFrame(ch, buf_ptr, input_len, src,u16Length);			
			break;
		}

		m_checksum ^= ch; 
		++ src;		
	}
	return NULL;
}

unsigned char UsbRxWatcher:: checkEscape(unsigned char ch)
{
	if ((ch == MUX_KEY_WORD ) &&
		(m_cTstFrameState != RS232_FRAME_KEYWORD) &&
		(m_cTstFrameState != RS232_FRAME_MD_DATA))// enter MUX state(0x5A) and save the old
	{ 
		m_cOldTstFrameState = m_cTstFrameState;
		m_cTstFrameState = RS232_FRAME_KEYWORD;
		return 1;
	}

	return 0;
}

unsigned char UsbRxWatcher::transferFrame(unsigned char * ch)
{
	unsigned char ret = 0;
	if (m_cTstFrameState == RS232_FRAME_KEYWORD)
	{
		switch(*ch)
		{
			case MUX_KEY_WORD:
			*ch = MUX_KEY_WORD;  //5A 5A->5A
				ret = 1;
				break;
			case 0x01:
			*ch = STX_OCTET; //5A 01->55
				ret = 1;
				break;
			default:
				break;
		}
		m_cTstFrameState = m_cOldTstFrameState;//leave MUX state and restore the state
	}
	
	return ret;
}



Frame * UsbRxWatcher::dispatchFrame(unsigned char ch, unsigned char *buf_ptr, unsigned int input_len, unsigned char *src,unsigned short &u16Length)
{
	META_LOG("[Meta] parse state: RS232_FRAME_TYPE: %x ", ch);

	if (ch == RS232_INJECT_PRIMITIVE_OCTET || ch == RS232_COMMAND_TYPE_OCTET )
	{
		m_cTstFrameState = RS232_FRAME_MD_DATA ;
		/* if the frame is modem side, we just write whole data to ccci port */
		META_LOG("[Meta] parse state:  nRS232_FRAME_MD_DATA--: %d, total %d",input_len, m_uFrameLength);
		return decodeMDFrame((void *)buf_ptr, input_len, ch,u16Length);		
	}
	else if(ch >= RS232_INJECT_PRIMITIVE_OCTETMODEM2 && ch <= RS232_INJECT_PRIMITIVE_OCTETMODEM2_END)//0xA0 ~ A7
	{
		m_md_index = (ch - RS232_INJECT_PRIMITIVE_OCTETMODEM2)+1; //A0--1(MD2)   A3--4(MD5)
		m_cTstFrameState = RS232_FRAME_MD_DATA ;
		/* if the frame is modem side, we just write whole data to ccci port */
		META_LOG("[Meta] parse state:  nRS232_FRAME_MD_DATA--: %d, total %d",input_len, m_uFrameLength);
		return decodeMDFrame((void *)buf_ptr, input_len, ch,u16Length);	
	}
	else if(ch >= RS232_COMMAND_TYPE_MD2_MEMORY_DUMP && ch<= RS232_COMMAND_TYPE_MD2_MEMORY_DUMP_END)//0xC0 ~ 0xC7
	{
		m_md_index = (ch - RS232_COMMAND_TYPE_MD2_MEMORY_DUMP)+1;
		m_cTstFrameState = RS232_FRAME_MD_DATA ;
		/* if the frame is modem side, we just write whole data to ccci port */
		META_LOG("[Meta] parse state:  nRS232_FRAME_MD_DATA--: %d, total %d",input_len, m_uFrameLength);
		return decodeMDFrame((void *)buf_ptr, input_len, ch,u16Length);	
	}
	else if (ch ==RS232_INJECT_APPRIMITIVE_OCTET)//0x66
	{
		m_cTstFrameState = RS232_FRAME_AP_INJECT_PIRIMITIVE_HEADER;
		m_sRs232Frame.received_prig_header_length = 0;
		m_sRs232Frame.received_buf_para_length = 0;
		m_sRs232Frame.inject_prim.local_len = 0;
		m_sRs232Frame.inject_prim.peer_len = 0;
		m_sRs232Frame.header_ptr = (unsigned char*)&m_sRs232Frame.inject_prim;
		return decodeAPFrame( input_len,++src,u16Length);
		
	}
	else if(ch >= RS232_COMMAND_TYPE_MD_DATA_TUNNEL_START && 
			ch <= RS232_COMMAND_TYPE_MD_DATA_TUNNEL_END)//0xD0 ~ 0xD7
	{
		m_cTstFrameState = RS232_FRAME_MD_TUNNELING_DATA;
		m_md_index = ch - RS232_COMMAND_TYPE_MD_DATA_TUNNEL_START;
		return decodeLTE_C2KFrame(input_len,++src,ch,u16Length);
	}
	else
	{
		m_cTstFrameState = RS232_FRAME_STX; //error reset
		return NULL;
	}
	
	
}

Frame *UsbRxWatcher::decodeLTE_C2KFrame(unsigned int input_len,unsigned char * src,unsigned char frmType,unsigned short &u16Length)
{
	unsigned char * local_ptr = src;
	unsigned char	ch;
	int data_len = 0;
	unsigned short newlen = m_uFrameLength;

	
	META_LOG("[Meta] (decodeLTE_C2KFrame) input_len = %d, m_uFrameLength = %d",input_len,m_uFrameLength);
	
	while(u16Length!=input_len)
	{
		ch = *local_ptr;
		++data_len;	
		++ u16Length;
		
		if ((getComType() == META_UART_COM) && (data_len != newlen + 1))
		{
			if(ch == 0x77)
			{
				if(u16Length == input_len)
					return NULL;
				
				ch = *(++local_ptr);
				++ data_len;	
				++ u16Length;
				
				if(getUARTEsc(ch) == 0)
				{
					META_LOG("[Meta] incorrect UART ESC (%d)\n", ch);
					return NULL;
				}

				++ newlen;
			}			
		}
		
		if (data_len == newlen + 1) 
		{
			if(getMDChType(m_md_index) == FT_MODEM_CH_TUNNELING)
			{
				m_cTstFrameState = RS232_FRAME_STX;

				//to check whether checksum is ESC.
				if(input_len >= data_len + 3 + m_frm_len_byte)   //start byte + data len + data type +checksum
				{
					if((getComType() == META_UART_COM) && (ch == 0x77))
					{
						ch = *(++local_ptr); 
						++ u16Length;
						if(getUARTEsc(ch) == 0)
						{
							META_LOG("[Meta] [case1] incorrect checksum ESC (%d)\n", ch);
							return (new Frame());
						}
					}
					else if((ch == MUX_KEY_WORD))
				{
					ch = *(++local_ptr); 
						++ u16Length;
					if(ch == 0x01)
						ch = STX_OCTET;
					else if(ch == MUX_KEY_WORD)
						ch = MUX_KEY_WORD;					
					else
					{
							META_LOG("[Meta] [case2] incorrect checksum ESC (%d)\n", ch);
							return (new Frame());
						}
					}
				}

				META_LOG("[Meta] parse state: RS232_FRAME_CHECKSUM: checksum: %d, ch: %d",m_checksum, ch);
				
				if(m_checksum != ch)
				{
					META_LOG("[Meta] LTE modem frame checksum error!");
					return (new Frame());
				}
				
				m_checksum = STX_OCTET;
				META_LOG("[Meta] (decodeLTE_C2KFrame) u16Length2 = %d",u16Length);
				
			}
			int len = flowControl(src, newlen);
			return sendMdTask(src, len, frmType);
		}
		m_checksum ^= ch; 
		++local_ptr;				
	}
	return NULL;
}

unsigned char UsbRxWatcher::getUARTEsc(unsigned char &ch)
{
	int ret = 1;

	switch(ch)
	{
	case 0x01:
		ch = 0x11;
		break;
	case 0x02:
		ch = 0x13;
		break;
	case 0x03:
		ch = 0x77;
		break;
	default:
		ret = 0;
		break;
	}

	return ret;
}


//////////////////////////////////////////////////////////////////////////
