/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.manifest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Manifest token tree. Manifest indentation forms a DAG structure, 
 * used for further translation to JSON format. 
 * 
 * Supported YAML constructs:
 * block sequences, mappings, scalars, comments, symbols.
 */
public class ManifestNode {
	private List<ManifestNode> children;
	private String label;
	private int depth;

	public ManifestNode(String label, int depth) {
		this.children = new ArrayList<ManifestNode>();
		this.label = label.trim();
		this.depth = depth;
	}

	public List<ManifestNode> getChildren() {
		return children;
	}

	public String getLabel() {
		return label;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int depth = getDepth();

		while (depth-- > 0)
			sb.append(" ");

		sb.append(getLabel());
		sb.append(System.getProperty("line.separator"));

		for (ManifestNode child : getChildren())
			sb.append(child.toString());

		return sb.toString();
	}

	/**
	 * Parses a JSON representation from the manifest tree.
	 */
	public JSONObject toJSON(URI target) throws ParseException {
		try {
			if (getChildren().isEmpty()) {
				/* leaf node */
				JSONObject res = new JSONObject();
				String[] sLabel = ManifestUtils.splitLabel(label);
				res.put(sLabel[0], ManifestUtils.normalize(ManifestUtils.resolve(sLabel[1], target)));
				return res;
			}

			if (isArrayNode()) {
				ArrayList<ArrayList<ManifestNode>> arrayItems = splitSequenceItems();
				JSONArray arrayRepresentation = new JSONArray();

				for (ArrayList<ManifestNode> item : arrayItems) {

					JSONObject itemRepresentation = new JSONObject();
					for (ManifestNode fieldNode : item) {
						JSONObject representation = fieldNode.toJSON(target);
						String key = JSONObject.getNames(representation)[0];
						itemRepresentation.put(key, representation.get(key));
					}

					arrayRepresentation.put(itemRepresentation);
				}

				JSONObject res = new JSONObject();
				String[] sLabel = ManifestUtils.splitLabel(label);
				res.put(sLabel[0], arrayRepresentation);
				return res;
			}

			JSONObject inner = new JSONObject();
			for (ManifestNode child : getChildren()) {
				JSONObject childNode = child.toJSON(target);
				String key = JSONObject.getNames(childNode)[0];
				inner.put(key, childNode.get(key));
			}

			JSONObject res = new JSONObject();
			String[] sLabel = ManifestUtils.splitLabel(label);
			res.put(sLabel[0], inner);
			return res;
		} catch (Exception ex) {
			/* corrupted or unsupported manifest format, admit failure */
			throw new ParseException("Corrupted or unsupported manifest format");
		}
	}

	private boolean isSequenceItem() {
		return label.startsWith("- ");
	}

	private boolean isArrayNode() {
		ManifestNode firstChild = getChildren().get(0);
		return firstChild.isSequenceItem();
	}

	/**
	 * Splits sequence items into separate array objects.
	 */
	private ArrayList<ArrayList<ManifestNode>> splitSequenceItems() {
		ArrayList<ArrayList<ManifestNode>> res = new ArrayList<ArrayList<ManifestNode>>();

		for (ManifestNode child : getChildren()) {
			if (child.isSequenceItem()) {
				ArrayList<ManifestNode> seqItemList = new ArrayList<ManifestNode>();
				seqItemList.add(child);
				res.add(seqItemList);
			} else {
				ArrayList<ManifestNode> seqItemList = res.get(res.size() - 1);
				seqItemList.add(child);
			}
		}

		return res;
	}
}