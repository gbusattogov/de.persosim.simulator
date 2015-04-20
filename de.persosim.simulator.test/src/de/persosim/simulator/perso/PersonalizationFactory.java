package de.persosim.simulator.perso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class PersonalizationFactory {
	
public static void marshal(Personalization pers, String path) {
		
		XStream xstream = getXStream();
		
		String xml = xstream.toXML(pers);
		
		xml = xml.replaceAll("class=\"org.*[Kk]ey\"", "");

		// Write to File
		File xmlFile = new File(path);
		xmlFile.getParentFile().mkdirs();
		
		StringWriter writer = new StringWriter();

		TransformerFactory ft = TransformerFactory.newInstance();
		ft.setAttribute("indent-number", new Integer(2));
		
		Transformer transformer;
		try {
			transformer = ft.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new StreamSource(new StringReader(xml)),
					new StreamResult(writer));

			OutputStreamWriter char_output = new OutputStreamWriter(
					new FileOutputStream(xmlFile), "UTF-8");
			char_output.append(writer.getBuffer());
			char_output.flush();
			char_output.close();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Personalization unmarchal(String path) {
		
		XStream xstream = getXStream();
		
		File xmlFile = new File(path);
		xmlFile.getParentFile().mkdirs();
		
		// get variables from our xml file, created before
		Personalization unmarshalledPerso = (Personalization) xstream.fromXML(xmlFile);
		
		return unmarshalledPerso;
	}
	
	
	public static XStream getXStream() {
		
		XStream xstream = new XStream(new DomDriver("UTF8"))
		{
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) 
			{
				return new MapperWrapper(next) {
					@SuppressWarnings("rawtypes")
					public boolean shouldSerializeMember(Class definedIn,
							String fieldName) {

						if (definedIn.getName().equals("de.persosim.simulator.perso.AbstractProfile")) {
							return false;
						}
						return super
								.shouldSerializeMember(definedIn, fieldName);
					}
				};
			}
		};

		xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
		xstream.setMode(XStream.ID_REFERENCES);

		
		xstream.registerConverter(new EncodedByteArrayConverter());
		xstream.registerConverter(new ProtocolAdapter());
		xstream.registerConverter(new KeyAdapter());
		
		return xstream;
	}

}